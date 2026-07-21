package ru.dialogsocnetwork.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import ru.dialogsocnetwork.conf.ProducerConfig
import ru.dialogsocnetwork.kafka.KafkaProducerLive.dtoSerde
import zio.json.{DecoderOps, EncoderOps}
import zio.kafka.producer.Producer
import zio.kafka.serde.Serde
import zio.{Task, URLayer, ZIO, ZLayer}

import java.util.UUID

case class KafkaProducerLive(producer: Producer, producerConfig: ProducerConfig)
    extends KafkaProducer:

  override def produce(key: UUID, msg: SagaEvent): Task[Unit] = producer
    .produce[Any, String, SagaEvent](
      new ProducerRecord(producerConfig.topic, key.toString, msg),
      Serde.string,
      dtoSerde
    )
    .unit

object KafkaProducerLive:

  val layer: URLayer[
    Producer & ProducerConfig,
    KafkaProducer
  ] =
    ZLayer.fromFunction(KafkaProducerLive.apply _)

  val dtoSerde: Serde[Any, SagaEvent] =
    Serde.string.inmapZIO[Any, SagaEvent](s =>
      ZIO
        .fromEither(s.fromJson[SagaEvent])
        .tapError(err => ZIO.logError(err))
        .mapError(new RuntimeException(_))
    )(dto => ZIO.attempt(dto.toJson))
