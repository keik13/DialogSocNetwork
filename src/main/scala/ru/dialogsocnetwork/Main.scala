package ru.dialogsocnetwork

import ru.dialogsocnetwork.auth.JwtServiceLive
import ru.dialogsocnetwork.conf.Configuration
import ru.dialogsocnetwork.kafka.{KafkaProducerLive, KafkaSettings}
import ru.dialogsocnetwork.redis.RedisClientJedis
import ru.dialogsocnetwork.server.{AuthMiddleware, DialogSocNetworkServer}
import ru.dialogsocnetwork.service.DialogMessageServiceLive
import zio.*
import zio.kafka.producer.Producer
import zio.logging.{ConsoleLoggerConfig, LogFilter, LogFormat, consoleLogger}

object Main extends ZIOAppDefault:

  private val customLogger = consoleLogger(
    ConsoleLoggerConfig(
      LogFormat.colored,
      LogFilter.LogLevelByNameConfig(
        LogLevel.Info, // Уровень для всех логов по умолчанию
        "ru.dialogsocnetwork" -> LogLevel.Trace // Для модуля dialogsocnetwork
      )
    )
  )

  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    Runtime.removeDefaultLoggers ++ customLogger

  override val run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .serviceWithZIO[DialogSocNetworkServer](_.start)
      .provide(
        Configuration.layer,
        DialogSocNetworkServer.layer,
        JwtServiceLive.layer,
        DialogMessageServiceLive.layer,
        RedisClientJedis.layer,
        AuthMiddleware.layer,
        Producer.live,
        KafkaProducerLive.layer,
        KafkaSettings.producerSettingsLayer
      )
