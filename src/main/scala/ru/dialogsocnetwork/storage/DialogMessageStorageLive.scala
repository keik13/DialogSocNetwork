package ru.dialogsocnetwork.storage

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.{Task, URLayer, ZLayer}

import java.util.UUID

final case class DialogMessageRow(
    id: Long,
    userId: UUID,
    toUserId: UUID,
    dialogId: String,
    message: String,
    createdAt: Long
)

final case class DialogMessageStorageLive(quill: Quill.Postgres[SnakeCase])
    extends DialogMessageStorage:

  import quill.*

  private inline def queryDialogMessage = quote(
    querySchema[DialogMessageRow](entity = "soc_dialog")
  )

  override def add(row: DialogMessageRow): Task[Unit] = run(
    queryDialogMessage.insert(
      _.userId -> lift(row.userId),
      _.toUserId -> lift(row.toUserId),
      _.dialogId -> lift(row.dialogId),
      _.message -> lift(row.message),
      _.createdAt -> lift(row.createdAt)
    )
  ).unit

  override def getDialog(
      dialogUserId: String,
      dialogToUserId: String
  ): Task[List[DialogMessageRow]] =
    run(
//      queryDialogMessage
//        .filter(_.userId == lift(userId))
//        .filter(_.toUserId == lift(toUserId))
//        .unionAll(
//          queryDialogMessage
//            .filter(_.userId == lift(toUserId))
//            .filter(_.toUserId == lift(userId))
//        )
//        .sortBy(_.createdAt)(Ord.asc)
      queryDialogMessage
        .filter(_.dialogId == lift(dialogUserId))
        .unionAll(
          queryDialogMessage.filter(_.dialogId == lift(dialogToUserId))
        )
        .sortBy(_.createdAt)(Ord.asc)
    )

object DialogMessageStorageLive:
  val layer: URLayer[Quill.Postgres[SnakeCase], DialogMessageStorage] =
    ZLayer.fromFunction(DialogMessageStorageLive.apply _)
