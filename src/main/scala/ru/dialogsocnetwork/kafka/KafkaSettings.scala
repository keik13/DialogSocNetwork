package ru.dialogsocnetwork.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import ru.dialogsocnetwork.conf.ProducerConfig as MyProducerConfig
import zio.kafka.producer.ProducerSettings
import zio.{URLayer, ZIO, ZLayer}

object KafkaSettings:

  val producerSettingsLayer: URLayer[MyProducerConfig, ProducerSettings] =
    ZLayer.fromZIO(
      for
        config <- ZIO.service[MyProducerConfig]
        producerSettings <- ZIO.succeed(
          ProducerSettings(
            config.bootstrapServers.split(",").map(_.trim).toList
          )
            .withProperties(
              CommonClientConfigs.SECURITY_PROTOCOL_CONFIG -> config.securityProtocol,
              ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG -> "false",
              ProducerConfig.MAX_BLOCK_MS_CONFIG -> config.maxBlock.toMillis.toInt.toString,
              ProducerConfig.RETRIES_CONFIG -> config.retries.toString,
              ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG -> config.deliveryTimeout.toMillis.toInt.toString,
              ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG -> config.requestTimeout.toMillis.toInt.toString
            )
        )
      yield producerSettings
    )
