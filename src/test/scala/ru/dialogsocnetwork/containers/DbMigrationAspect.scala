package ru.dialogsocnetwork.containers

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import zio.ZIO
import zio.test.TestAspect
import zio.test.TestAspect.beforeAll

import javax.sql.DataSource

object DbMigrationAspect:

  type ConfigurationCallback = FluentConfiguration => FluentConfiguration

  private def doMigrate(
      ds: DataSource,
      configureCallback: ConfigurationCallback,
      locations: String*
  ) =
    ZIO.attempt {
      val flyway = configureCallback {
        val flyway = Flyway
          .configure()
          .dataSource(ds)

        if locations.nonEmpty then flyway.locations(locations*)
        else flyway
      }
        .load()
      flyway.migrate
    }

  def migrateOnce(migrationLocations: String*)(
      configureCallback: ConfigurationCallback = identity
  ): TestAspect[Nothing, DataSource, Nothing, Any] =
    beforeAll(
      ZIO
        .serviceWithZIO[DataSource](ds =>
          doMigrate(ds, configureCallback, migrationLocations*)
        )
        .orDie
    )
