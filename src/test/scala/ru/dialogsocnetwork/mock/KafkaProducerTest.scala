package ru.dialogsocnetwork.mock

import ru.dialogsocnetwork.kafka.{KafkaProducer, SagaEvent}
import zio.{Task, ULayer, ZIO, ZLayer}

import java.util.UUID

case class KafkaProducerTest() extends KafkaProducer:

  override def produce(key: UUID, msg: SagaEvent): Task[Unit] = ZIO.unit

object KafkaProducerTest:

  val layer: ULayer[KafkaProducer] =
    ZLayer.fromFunction(KafkaProducerTest.apply _)
