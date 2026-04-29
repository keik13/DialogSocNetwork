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
              .mapBoth(
                _ => Response.unauthorized,
                userInfo => (request, userInfo)
              )
          case _ =>
            ZIO.fail(
              Response.unauthorized.addHeaders(
                Headers(Header.WWWAuthenticate.Bearer("realm"))
              )
            )
      }
    }

object AuthMiddleware:
  val layer: URLayer[JwtService, AuthMiddleware] = ZLayer.derive[AuthMiddleware]
