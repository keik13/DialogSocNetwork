package ru.dialogsocnetwork.server

import ru.dialogsocnetwork.auth.{JwtService, UserInfo}
import zio.*
import zio.http.*

final case class AuthMiddleware(jwtService: JwtService):

  val jwtAuthentication: HandlerAspect[Any, UserInfo] =
    HandlerAspect.interceptIncomingHandler {
      handler { (request: Request) =>
        request.header(Header.Authorization) match
          case Some(Header.Authorization.Bearer(token)) =>
            jwtService
              .verify(token.value.asString)
              .tapError(err =>
                ZIO.logError(s"Error Authorization ${err.getMessage}")
              )
              .mapBoth(
                _ => Response.unauthorized,
                userInfo => (request, userInfo)
              )
          case _ =>
            ZIO.logError(s"Error Authorization: No Header Authorization") *> ZIO
              .fail(
                Response.unauthorized
              )
      }
    }

object AuthMiddleware:
  val layer: URLayer[JwtService, AuthMiddleware] = ZLayer.derive[AuthMiddleware]
