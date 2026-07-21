package ru.dialogsocnetwork.kafka

import zio.json.JsonCodec

import java.util.UUID

enum SagaEvent derives JsonCodec:
  case MessageSent(msgId: UUID, dialogId: String, toUserId: UUID)
      extends SagaEvent
  case MessageRejected(msgId: UUID, dialogId: String, toUserId: UUID)
      extends SagaEvent
  case MessageRead(msgId: UUID, dialogId: String, toUserId: UUID, count: Int)
      extends SagaEvent
