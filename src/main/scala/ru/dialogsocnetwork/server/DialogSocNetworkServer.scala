package ru.dialogsocnetwork.server

import ru.dialogsocnetwork.api.{DialogMessage, DialogMessageText, ErrorResponse}
import ru.dialogsocnetwork.auth.UserInfo
import ru.dialogsocnetwork.redis.RedisClient
import ru.dialogsocnetwork.server.DialogSocNetworkServer.parseBody
import ru.dialogsocnetwork.server.RequestIdMiddleware.requestId
import ru.dialogsocnetwork.service.*
import ru.dialogsocnetwork.util.{InvalidBody, InvalidToken, MissingParams, MissingXRequestId}
import zio.http.*
import zio.json.{EncoderOps, JsonDecoder, JsonEncoder}
import zio.{IO, URLayer, ZIO, ZLayer}

import java.util.UUID

final case class DialogSocNetworkServer(
    authMiddleware: AuthMiddleware,
    dialogMessageService: DialogMessageService,
    redis: RedisClient
):

  private val dialogRoutes =
    Routes(
      Method.POST / "dialog" / uuid("userId") / "send" -> handler {
        (userId: UUID, req: Request) =>
          withContext { (user: UserInfo) =>
            for
              e <- parseBody[DialogMessageText](req)
              requestId <- requestId
              _ <- ZIO.logTrace(
                s"DialogSocNetwork server dialog $userId send with X-Request-ID $requestId"
              )
              r <- dialogMessageService.add(e, user.userId, userId)
            yield Response.ok
          }
      },
      Method.GET / "dialog" / uuid("userId") / "list" -> handler {
        (userId: UUID, req: Request) =>
          withContext { (user: UserInfo) =>
            for
              requestId <- requestId
              _ <- ZIO.logTrace(
                s"DialogSocNetwork server dialog $userId list with X-Request-ID $requestId"
              )
              r <- dialogMessageService.getById(user.userId, userId)
            yield Response.json(r.toJson)
          }
      }
    )

  private val app = (dialogRoutes @@ authMiddleware.jwtAuthentication @@ RequestIdMiddleware.requestIdMiddleware)
    .handleErrorZIO {
      case InvalidBody =>
        ZIO
          .logError("InvalidBody")
          .as(Response.badRequest)
      case InvalidToken =>
        ZIO
          .logError("InvalidToken")
          .as(Response.badRequest)
      case MissingParams =>
        ZIO
          .logError("MissingParams")
          .as(Response.badRequest)
      case MissingXRequestId =>
        ZIO
          .logError("MissingXRequestId")
          .as(Response.badRequest)
      case err: ErrorResponse =>
        ZIO
          .logError(err.message)
          .as(
            Response.json(err.toJson).status(Status.InternalServerError)
          )
      case err: Throwable =>
        ZIO
          .logError(err.getMessage)
          .as(
            Response
              .json(ErrorResponse(err.getMessage, "", 500).toJson)
              .status(Status.InternalServerError)
          )
    }

  private def run: ZIO[Any, Throwable, Nothing] = Server
    .serve(app)
    .provide(Server.defaultWithPort(8081))
    .tapError(err => ZIO.logError(err.getMessage))

  def start: ZIO[Any, Throwable, Unit] =
    for
      libName <- redis.functionLoadReplace()
      _ <- ZIO.logInfo(s"$libName loaded to Redis!")
      _ <- run
    yield ()

object DialogSocNetworkServer:
  val layer: URLayer[
    RedisClient with DialogMessageService with AuthMiddleware,
    DialogSocNetworkServer
  ] =
    ZLayer.fromFunction(DialogSocNetworkServer.apply _)

  def parseBody[A: JsonDecoder](request: Request): IO[InvalidBody.type, A] =
    request.body
      .asJsonFromCodec[A]
      .tapError(err => ZIO.logError(err.getMessage))
      .orElseFail(InvalidBody)

  def fromOption[A: JsonEncoder](opt: Option[A]): Response =
    opt match
      case Some(value) => Response.json(value.toJson)
      case None        => Response.notFound

  def searchParams(
      firstParam: Option[String],
      secondParam: Option[String]
  ): Option[(String, String)] =
    (firstParam, secondParam) match
      case (Some(fn), Some(ln)) => Some((fn, ln))
      case _                    => None
