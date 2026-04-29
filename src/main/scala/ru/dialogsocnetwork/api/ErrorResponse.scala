package ru.dialogsocnetwork.api

import zio.json.*

@jsonMemberNames(SnakeCase)
final case class ErrorResponse(message: String, requestId: String, code: Int)
    derives JsonEncoder
