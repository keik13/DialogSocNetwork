package ru.dialogsocnetwork.service

import ru.dialogsocnetwork.api.{DialogMessage, DialogMessageText}
import ru.dialogsocnetwork.kafka.KafkaProducer
import ru.dialogsocnetwork.kafka.SagaEvent.{
  MessageRead,
  MessageRejected,
  MessageSent
}
import ru.dialogsocnetwork.redis.RedisClient
import ru.dialogsocnetwork.server.RequestIdMiddleware.requestId
import ru.dialogsocnetwork.service.DialogMessageServiceLive.getDialogId
import zio.{Clock, Random, Task, URLayer, ZIO, ZLayer}

import java.util.UUID
import scala.concurrent.duration.SECONDS

final case class DialogMessageServiceLive(
    redis: RedisClient,
    producer: KafkaProducer
) extends DialogMessageService:

  override def add(
      request: DialogMessageText,
      userId: UUID,
      toUserId: UUID
  ): Task[Unit] =
    for
      uuid <- Random.nextUUID
      _ <- producer.produce(
        uuid,
        MessageSent(uuid, getDialogId(userId, toUserId), toUserId)
      )
      _ <- Clock
        .currentTime(SECONDS)
        .flatMap(ct =>
          redis.xAdd(
            getDialogId(userId, toUserId),
            Map(
              "msgId" -> uuid.toString,
              "userId" -> userId.toString,
              "toUserId" -> toUserId.toString,
              "text" -> request.text,
              "createdAt" -> ct.toString
            )
          )
        )
        .catchAll(redisError =>
          ZIO.logError(
            s"Redis failed: $redisError. Initiating compensation saga..."
          ) *>
            producer.produce(
              uuid,
              MessageRejected(uuid, getDialogId(userId, toUserId), toUserId)
            ) *>
            ZIO.fail(
              new RuntimeException(
                "Saga aborted: Local database write failed. Compensation sent."
              )
            )
        )
      requestId <- requestId
      _ <- ZIO.logTrace(
        s"DialogSocNetwork server dialog $toUserId added with X-Request-ID $requestId"
      )
    yield ()

  override def getById(
      userId: UUID,
      toUserId: UUID
  ): Task[List[DialogMessage]] =
    for
      r <- redis.xRevRange(getDialogId(userId, toUserId)) <&> redis.hGet(
        getDialogId(userId, toUserId),
        toUserId.toString
      )
      (streamMessages, toUserLastReadIdOpt) = r
      toUserLastReadId = toUserLastReadIdOpt.getOrElse("0-0")

      requestId <- requestId
      _ <- ZIO.logTrace(
        s"DialogSocNetwork server dialog $toUserId listed with X-Request-ID $requestId"
      )
    yield streamMessages.reverse.map(e =>
      DialogMessage(
        e.getID.toString,
        UUID.fromString(e.getFields.get("msgId")),
        UUID.fromString(e.getFields.get("userId")),
        UUID.fromString(e.getFields.get("toUserId")),
        e.getFields.get("text"),
        UUID.fromString(e.getFields.get("userId")) == userId || e.getID.toString <= toUserLastReadId
      )
    )

  override def addF(
      request: DialogMessageText,
      userId: UUID,
      toUserId: UUID
  ): Task[Unit] = Clock
    .currentTime(SECONDS)
    .flatMap(ct =>
      redis.xAddFCall(
        getDialogId(userId, toUserId),
        Map(
          "userId" -> userId.toString,
          "toUserId" -> toUserId.toString,
          "text" -> request.text,
          "createdAt" -> ct.toString
        )
      )
    )

  override def getByIdF(
      userId: UUID,
      toUserId: UUID
  ): Task[List[DialogMessage]] = redis
    .xRevRangeFCall(getDialogId(userId, toUserId))
    .map(
      _.reverse.map(e =>
        DialogMessage(
          "",
          UUID.randomUUID(),
          UUID.fromString(e(1)),
          UUID.fromString(e(3)),
          e(5),
          false
        )
      )
    )

  override def markAsRead(
      userId: UUID,
      toUserId: UUID,
      id: String,
      msgId: UUID
  ): Task[Unit] =
    for
      // 1. Получаем предыдущий ID прочтения
      oldMessageIdOpt <- redis.hGet(
        getDialogId(userId, toUserId),
        userId.toString
      )
      oldMessageId = oldMessageIdOpt.getOrElse("0-0")

      // 2. Считаем количество сообщений между старым и новым ID прочтения
      // В Redis диапазон задается как (start, end]. Знак ( делает старый ID исключающим.
      messagesInRange <- redis.xRange(
        getDialogId(userId, toUserId),
        s"($oldMessageId",
        msgId.toString
      )
      readCount = messagesInRange.size

      // Если readCount == 0, значит пользователь шлет дублирующий запрос, ничего делать не нужно
      _ <- ZIO.when(readCount > 0) {
        for
          // ТРАНЗАКЦИЯ 1: Локально сохраняем новый ID
          _ <- redis.hSet(
            getDialogId(userId, toUserId),
            Map(userId.toString -> msgId.toString)
          )

          // ТРАНЗАКЦИЯ 2: Публикуем событие Саги с ТОЧНЫМ количеством для декремента
          _ <- producer
            .produce(
              msgId,
              MessageRead(
                msgId,
                getDialogId(userId, toUserId),
                toUserId,
                readCount
              )
            )
            .tapError { error =>
              // КОМПЕНСАЦИЯ: Если Kafka упала, возвращаем старый ID прочтения назад в Redis
              ZIO.logError(
                s"Kafka failed. Rolling back read status to $oldMessageId"
              ) *>
                redis.hSet(
                  getDialogId(userId, toUserId),
                  Map(userId.toString -> oldMessageId)
                ) *>
                ZIO.fail(error)
            }
        yield ()
      }
    yield ()

object DialogMessageServiceLive:
  val layer: URLayer[RedisClient with KafkaProducer, DialogMessageService] =
    ZLayer.fromFunction(DialogMessageServiceLive.apply _)

  // Генерация детерминированного ID диалога
  private def getDialogId(userId: UUID, toUserId: UUID): String =
    if userId.compareTo(toUserId) < 0 then s"${userId}_$toUserId"
    else s"${toUserId}_$userId"
