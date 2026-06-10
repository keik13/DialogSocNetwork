package ru.dialogsocnetwork.util

trait CustomError extends Throwable

case object InvalidBody extends CustomError

case object InvalidToken extends CustomError

case object MissingParams extends CustomError

case object MissingXRequestId extends CustomError
