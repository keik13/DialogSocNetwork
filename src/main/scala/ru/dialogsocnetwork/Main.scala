package ru.dialogsocnetwork

import ru.dialogsocnetwork.auth.JwtServiceLive
import ru.dialogsocnetwork.conf.Configuration
import ru.dialogsocnetwork.db.{Db, DbMigrator}
import ru.dialogsocnetwork.server.{AuthMiddleware, DialogSocNetworkServer}
import ru.dialogsocnetwork.service.DialogMessageServiceLive
import ru.dialogsocnetwork.storage.DialogMessageStorageLive
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object Main extends ZIOAppDefault:

  override val run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .serviceWithZIO[DialogSocNetworkServer](_.start)
      .provide(
        Configuration.layer,
        Db.dataSourceLayer,
        Db.quillLayer,
        DbMigrator.layer,
        DialogSocNetworkServer.layer,
        JwtServiceLive.layer,
        DialogMessageServiceLive.layer,
        DialogMessageStorageLive.layer,
        AuthMiddleware.layer
      )
