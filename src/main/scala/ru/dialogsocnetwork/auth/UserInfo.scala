package ru.dialogsocnetwork.auth

import zio.json.JsonCodec

import java.util.UUID

final case class UserInfo(userId: UUID) derives JsonCodec
