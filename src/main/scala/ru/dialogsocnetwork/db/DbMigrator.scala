package ru.dialogsocnetwork.db

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateErrorResult
import zio.{Task, URLayer, ZIO, ZLayer}

import javax.sql.DataSource

final case class DbMigrator(ds: DataSource):

  val migrate: Task[Unit] =
    (for
      flyway <- loadFlyway
      migrateResult <- ZIO.attempt(flyway.migrate())
      migrateResult <- migrateResult match
        case r: MigrateErrorResult =>
          ZIO.fail(DbMigrationFailed(r.error.message, r.error.stackTrace))
        case _ => ZIO.unit
    yield migrateResult)
      .onError(cause =>
        ZIO.logErrorCause("Database migration has failed", cause)
      )

  private lazy val loadFlyway: Task[Flyway] =
    ZIO.attempt {
      Flyway
        .configure()
        .dataSource(ds)
        .load()
    }

case class DbMigrationFailed(msg: String, stackTrace: String)
    extends RuntimeException(s"$msg\n$stackTrace")

object DbMigrator:
  def layer: URLayer[DataSource, DbMigrator] =
    ZLayer.fromFunction(DbMigrator(_))
