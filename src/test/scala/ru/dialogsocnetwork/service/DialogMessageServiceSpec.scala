package ru.dialogsocnetwork.service

import ru.dialogsocnetwork.api.{DialogMessage, DialogMessageText}
import ru.dialogsocnetwork.containers.{Containers, DbMigrationAspect}
import ru.dialogsocnetwork.db.Db
import ru.dialogsocnetwork.storage.DialogMessageStorageLive
import zio.test.*
import zio.test.TestAspect.sequential
import zio.{Random, ZIO, ZLayer}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

object DialogMessageServiceSpec extends ZIOSpecDefault:

  override def spec: Spec[TestEnvironment, Throwable] = {
    suite("DialogMessageService")(
      test("should add messages to user") {
        for
          service <- ZIO.service[DialogMessageService]
          userId <- Random.nextUUID
          toUserId <- Random.nextUUID
          _ <- service.add(
            DialogMessageText(
              "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
            ),
            userId,
            toUserId
          )
          _ <- TestClock.setTime(Instant.now.plus(1, ChronoUnit.HOURS))
          _ <- service.add(
            DialogMessageText(
              "Брат, я ничего не понял, брат."
            ),
            toUserId,
            userId
          )
          _ <- TestClock.setTime(Instant.now.plus(2, ChronoUnit.HOURS))
          _ <- service.add(
            DialogMessageText(
              "Lectus mauris ultrices eros in cursus turpis massa. In fermentum et sollicitudin ac orci."
            ),
            userId,
            toUserId
          )
          dialog <- service.getById(userId, toUserId)
        yield assertTrue(
          dialog == List(
            DialogMessage(
              UUID.fromString("b2c8ccb8-191a-4233-9b34-3e3111a4adaf"),
              UUID.fromString("014a363e-7a00-48d5-b154-dc024003f3d1"),
              "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
            ),
            DialogMessage(
              UUID.fromString("014a363e-7a00-48d5-b154-dc024003f3d1"),
              UUID.fromString("b2c8ccb8-191a-4233-9b34-3e3111a4adaf"),
              "Брат, я ничего не понял, брат."
            ),
            DialogMessage(
              UUID.fromString("b2c8ccb8-191a-4233-9b34-3e3111a4adaf"),
              UUID.fromString("014a363e-7a00-48d5-b154-dc024003f3d1"),
              "Lectus mauris ultrices eros in cursus turpis massa. In fermentum et sollicitudin ac orci."
            )
          )
        )
      }
    ) @@ DbMigrationAspect.migrateOnce(
      "filesystem:src/test/resources/db/migration"
    )()
  }
    .provideShared(
      DialogMessageServiceLive.layer,
      DialogMessageStorageLive.layer,
      Containers.layer,
      Containers.postgresLayer,
      Db.dataSourceLayer,
      Db.quillLayer
    ) @@ sequential
