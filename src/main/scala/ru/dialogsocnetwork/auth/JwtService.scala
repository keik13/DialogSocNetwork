package ru.dialogsocnetwork.auth

import zio.*

trait JwtService:
  def verify(token: String): Task[UserInfo]
