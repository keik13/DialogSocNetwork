package ru.dialogsocnetwork.api

import ru.dialogsocnetwork.util.CustomError
import zio.json.*

@jsonMemberNames(SnakeCase)
final case class ErrorResponse(message: String, requestId: String, code: Int)
    extends CustomError derives JsonEncoder
