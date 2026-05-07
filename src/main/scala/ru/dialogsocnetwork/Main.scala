package ru.dialogsocnetwork

import ru.dialogsocnetwork.auth.JwtServiceLive
import ru.dialogsocnetwork.conf.Configuration
import ru.dialogsocnetwork.redis.RedisClientJedis
import ru.dialogsocnetwork.server.{AuthMiddleware, DialogSocNetworkServer}
import ru.dialogsocnetwork.service.DialogMessageServiceLive

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object Main extends ZIOAppDefault:

  override val run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .serviceWithZIO[DialogSocNetworkServer](_.start)
      .provide(
        Configuration.layer,
        DialogSocNetworkServer.layer,
        JwtServiceLive.layer,
        DialogMessageServiceLive.layer,
        RedisClientJedis.layer,
        AuthMiddleware.layer
      )
