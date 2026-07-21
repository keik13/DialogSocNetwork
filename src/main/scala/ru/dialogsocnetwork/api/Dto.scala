package ru.dialogsocnetwork.api

import zio.json.*

import java.util.UUID

@jsonMemberNames(SnakeCase)
final case class DialogMessageText(text: String) derives JsonDecoder

@jsonMemberNames(SnakeCase)
final case class MessageRead(id: String, msgId: UUID) derives JsonDecoder

@jsonMemberNames(SnakeCase)
final case class DialogMessage(
    id: String,
    msgId: UUID,
    from: UUID,
    to: UUID,
    text: String,
    isRead: Boolean
) derives JsonEncoder
