package ru.dialogsocnetwork.service

import ru.dialogsocnetwork.api.{DialogMessage, DialogMessageText}
import ru.dialogsocnetwork.redis.RedisClient
import ru.dialogsocnetwork.server.RequestIdMiddleware.requestId
import ru.dialogsocnetwork.service.DialogMessageServiceLive.getDialogId
import zio.{Clock, Task, URLayer, ZIO, ZLayer}

import java.util.UUID
import scala.concurrent.duration.SECONDS

final case class DialogMessageServiceLive(
    redis: RedisClient
) extends DialogMessageService:
  override def add(
      request: DialogMessageText,
      userId: UUID,
      toUserId: UUID
  ): Task[Unit] =
    for
      _ <- Clock
        .currentTime(SECONDS)
        .flatMap(ct =>
          redis.xAdd(
            getDialogId(userId, toUserId),
            Map(
              "userId" -> userId.toString,
              "toUserId" -> toUserId.toString,
              "text" -> request.text,
              "createdAt" -> ct.toString
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
      r <- redis
        .xRevRange(getDialogId(userId, toUserId))
        .map(
          _.reverse.map(e =>
            DialogMessage(
              UUID.fromString(e.getFields.get("userId")),
              UUID.fromString(e.getFields.get("toUserId")),
              e.getFields.get("text")
            )
          )
        )
      requestId <- requestId
      _ <- ZIO.logTrace(
        s"DialogSocNetwork server dialog $toUserId listed with X-Request-ID $requestId"
      )
    yield r

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
          UUID.fromString(e(1)),
          UUID.fromString(e(3)),
          e(5)
        )
      )
    )

object DialogMessageServiceLive:
  val layer: URLayer[RedisClient, DialogMessageService] =
    ZLayer.fromFunction(DialogMessageServiceLive.apply _)

  // Генерация детерминированного ID диалога
  private def getDialogId(userId: UUID, toUserId: UUID): String =
    if userId.compareTo(toUserId) < 0 then s"${userId}_$toUserId"
    else s"${toUserId}_$userId"
