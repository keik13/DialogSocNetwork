package ru.dialogsocnetwork.service

import ru.dialogsocnetwork.api.{DialogMessage, DialogMessageText}
import ru.dialogsocnetwork.storage.{DialogMessageRow, DialogMessageStorage}
import zio.{Clock, Task, URLayer, ZLayer}
import scala.concurrent.duration.SECONDS
import java.util.UUID

final case class DialogMessageServiceLive(storage: DialogMessageStorage)
    extends DialogMessageService:
  override def add(
      request: DialogMessageText,
      userId: UUID,
      toUserId: UUID
  ): Task[Unit] =
    Clock
      .currentTime(SECONDS)
      .flatMap(ct =>
        storage.add(
          DialogMessageRow(
            0,
            userId,
            toUserId,
            userId.toString + toUserId.toString,
            request.text,
            ct
          )
        )
      )

  override def getById(
      userId: UUID,
      toUserId: UUID
  ): Task[List[DialogMessage]] = storage
    .getDialog(
      userId.toString + toUserId.toString,
      toUserId.toString + userId.toString
    )
    .map(_.map(r => DialogMessage(r.userId, r.toUserId, r.message)))

object DialogMessageServiceLive:
  val layer: URLayer[DialogMessageStorage, DialogMessageService] =
    ZLayer.fromFunction(DialogMessageServiceLive.apply _)
