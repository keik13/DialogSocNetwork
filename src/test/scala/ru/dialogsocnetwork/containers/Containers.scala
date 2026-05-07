package ru.dialogsocnetwork.containers

import com.redis.testcontainers.RedisContainer
import zio.{Duration, ULayer, ZIO, ZLayer}
import org.testcontainers.utility.DockerImageName
import ru.dialogsocnetwork.conf.{DbConfig, RedisCommonConfig}

object Containers:

  val redisLayer: ZLayer[Any, Throwable, RedisCommonConfig] = ZLayer.scoped {
    for
      container <- ZIO.acquireRelease(
        ZIO.attemptBlocking {
          val c =
            new RedisContainer(DockerImageName.parse("redis:8-alpine"))
          c.withStartupTimeout(Duration.fromSeconds(120)).start()
          c
        }
      )(c => ZIO.attemptBlocking(c.stop()).orDie)

      host = container.getHost
      port = container.getFirstMappedPort
    yield RedisCommonConfig(
      "standalone",
      s"$host:$port",
      "",
      5,
      5000,
      "master01"
    )
  }
