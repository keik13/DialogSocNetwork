package ru.dialogsocnetwork.kafka

import zio.Task

import java.util.UUID

trait KafkaProducer:

  def produce(key: UUID, msg: SagaEvent): Task[Unit]
