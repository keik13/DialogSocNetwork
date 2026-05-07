package ru.dialogsocnetwork.conf

import zio.config.magnolia.{DeriveConfig, deriveConfig}
import zio.{Config, Layer, ZLayer}

final case class RootConfig(
    config: AppConfig
)
final case class AppConfig(
    jwt: JwtConfig,
    redisCommonConfig: RedisCommonConfig
)

final case class DbConfig(
    url: String,
    user: String,
    password: String
)

case class RedisCommonConfig(
    mode: String,
    servers: String,
    password: String,
    maxAttempts: Int,
    timeoutMillis: Int,
    masterName: String
)

final case class JwtConfig(
    secret: String,
    issuer: String,
    expireInSeconds: Int
)

object Configuration:
  import zio.config.typesafe.*

  val layer: Layer[Config.Error, RedisCommonConfig with JwtConfig] =
    for
      appConfig <- ZLayer.fromZIO(
        TypesafeConfigProvider
          .fromResourcePath()
          .load(deriveConfig[RootConfig])
          .map(_.config)
      )
      l <- ZLayer.succeed(appConfig.get.redisCommonConfig) ++
        ZLayer.succeed(appConfig.get.jwt)
    yield l
