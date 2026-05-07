package ru.dialogsocnetwork.service

import ru.dialogsocnetwork.api.{DialogMessage, DialogMessageText}
import ru.dialogsocnetwork.redis.RedisClient
import ru.dialogsocnetwork.service.DialogMessageServiceLive.getDialogId
import zio.{Clock, Task, URLayer, ZLayer}

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
    Clock
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

  override def getById(
      userId: UUID,
      toUserId: UUID
  ): Task[List[DialogMessage]] = redis
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

object DialogMessageServiceLive:
  val layer: URLayer[RedisClient, DialogMessageService] =
    ZLayer.fromFunction(DialogMessageServiceLive.apply _)

  // Генерация детерминированного ID диалога
  private def getDialogId(userId: UUID, toUserId: UUID): String =
    if userId.compareTo(toUserId) < 0 then s"${userId}_$toUserId"
    else s"${toUserId}_$userId"
