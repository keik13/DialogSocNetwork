package ru.dialogsocnetwork.server

import ru.dialogsocnetwork.util.MissingXRequestId
import zio.Task
import zio.http.*

final case class RequestId(value: String)

object RequestIdMiddleware:

  val requestIdMiddleware: Middleware[Any] =
    new Middleware[Any]:
      override def apply[Env1 <: Any, Err](
          routes: Routes[Env1, Err]
      ): Routes[Env1, Err] =
        routes.transform { handler =>
          Handler.scoped[Env1] {
            Handler.fromFunctionZIO[Request] { req =>
              val requestIdValue = req
                .header[String]("X-Request-ID")
                .getOrElse(java.util.UUID.randomUUID().toString)
              RequestStore.set(RequestId(requestIdValue)) *> handler(req)
            }
          }
        }

  val requestId: Task[String] =
    RequestStore.get[RequestId].someOrFail(MissingXRequestId).map(_.value)
