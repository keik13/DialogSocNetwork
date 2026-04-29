package ru.dialogsocnetwork.server

import ru.dialogsocnetwork.api.{DialogMessageText, DialogMessage, ErrorResponse}
import ru.dialogsocnetwork.auth.UserInfo
import ru.dialogsocnetwork.db.DbMigrator
import ru.dialogsocnetwork.server.DialogSocNetworkServer.parseBody
import ru.dialogsocnetwork.service.*
import ru.dialogsocnetwork.util.{InvalidBody, InvalidToken, MissingParams}
import zio.http.*
import zio.json.{EncoderOps, JsonDecoder, JsonEncoder}
import zio.{IO, URLayer, ZIO, ZLayer}

import java.util.UUID

final case class DialogSocNetworkServer(
    migrator: DbMigrator,
    authMiddleware: AuthMiddleware,
    dialogMessageService: DialogMessageService
):

  private val dialogRoutes =
    Routes(
      Method.POST / "dialog" / uuid("userId") / "send" -> handler {
        (userId: UUID, req: Request) =>
          withContext { (user: UserInfo) =>
            for
              e <- parseBody[DialogMessageText](req)
              r <- dialogMessageService.add(e, user.userId, userId)
            yield Response.ok
          }
      },
      Method.GET / "dialog" / uuid("userId") / "list" -> handler {
        (userId: UUID, req: Request) =>
          withContext { (user: UserInfo) =>
            for r <- dialogMessageService.getById(user.userId, userId)
            yield Response.json(r.toJson)
          }
      }
    )

  private val app = (dialogRoutes @@ authMiddleware.jwtAuthentication)
    .handleErrorZIO {
      case InvalidBody | InvalidToken | MissingParams =>
        ZIO.succeed(Response.badRequest)
      case err: Throwable =>
        ZIO
          .logError(err.getMessage)
          .as(
            Response
              .json(
                ru.dialogsocnetwork.api
                  .ErrorResponse(err.getMessage, "", 0)
                  .toJson
              )
              .status(Status.InternalServerError)
          )
    }

  private def run: ZIO[Any, Throwable, Nothing] = Server
    .serve(app)
    .provide(Server.defaultWithPort(8081))
    .tapError(err => ZIO.logError(err.getMessage))

  def start: ZIO[Any, Throwable, Unit] =
    for
      _ <- migrator.migrate
      _ <- run
    yield ()

object DialogSocNetworkServer:
  val layer: URLayer[
    DbMigrator with DialogMessageService with AuthMiddleware,
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
