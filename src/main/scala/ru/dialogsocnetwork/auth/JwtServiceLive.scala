package ru.dialogsocnetwork.auth

import pdi.jwt.{JwtAlgorithm, JwtZIOJson}
import ru.dialogsocnetwork.conf.JwtConfig
import ru.dialogsocnetwork.util.InvalidToken
import zio.json.DecoderOps
import zio.{Task, URLayer, ZIO, ZLayer}

final case class JwtServiceLive(jwtConfig: JwtConfig) extends JwtService:

  override def verify(token: String): Task[UserInfo] =
    for
      jwt <- ZIO.fromTry(
        JwtZIOJson.decode(token, jwtConfig.secret, Seq(JwtAlgorithm.HS256))
      )
      r <- ZIO
        .from(jwt.content.fromJson[UserInfo])
        .tapError(err => ZIO.logError(err))
        .orElseFail(InvalidToken)
    yield r

object JwtServiceLive:
  val layer: URLayer[JwtConfig, JwtService] =
    ZLayer.fromFunction(JwtServiceLive.apply _)
