package ru.dialogsocnetwork.conf

import zio.config.magnolia.{DeriveConfig, deriveConfig}
import zio.{Config, Duration, Layer, ZLayer}

final case class RootConfig(
    config: AppConfig
)
final case class AppConfig(
    jwt: JwtConfig,
    redisCommonConfig: RedisCommonConfig,
    producerConfig: ProducerConfig
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

final case class ProducerConfig(
    topic: String,
    bootstrapServers: String,
    securityProtocol: String,
    retries: Int,
    maxBlock: Duration,
    deliveryTimeout: Duration,
    requestTimeout: Duration
)

object Configuration:
  import zio.config.typesafe.*

  val layer: Layer[
    Config.Error,
    RedisCommonConfig with JwtConfig with ProducerConfig
  ] =
    for
      appConfig <- ZLayer.fromZIO(
        TypesafeConfigProvider
          .fromResourcePath()
          .load(deriveConfig[RootConfig])
          .map(_.config)
      )
      l <- ZLayer.succeed(appConfig.get.redisCommonConfig) ++
        ZLayer.succeed(appConfig.get.jwt) ++
        ZLayer.succeed(appConfig.get.producerConfig)
    yield l
