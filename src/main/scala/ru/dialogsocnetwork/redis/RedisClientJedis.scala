package ru.dialogsocnetwork.redis

import redis.clients.jedis.resps.StreamEntry
import redis.clients.jedis.{
  DefaultJedisClientConfig,
  HostAndPort,
  JedisClientConfig,
  RedisClusterClient,
  RedisSentinelClient,
  StreamEntryID,
  UnifiedJedis,
  RedisClient as JedisRedisClient
}
import ru.dialogsocnetwork.conf.RedisCommonConfig
import zio.{RLayer, Task, ZIO, ZLayer}

import scala.jdk.CollectionConverters.{
  ListHasAsScala,
  MapHasAsJava,
  SetHasAsJava
}

case class RedisClientJedis(jc: UnifiedJedis) extends RedisClient:

  override def xAdd(dialogId: String, message: Map[String, String]): Task[Unit] =
    ZIO.attemptBlocking(
      jc.xadd(s"dialog:$dialogId", StreamEntryID.NEW_ENTRY, message.asJava)
    )

  override def xRevRange(dialogId: String): Task[List[StreamEntry]] =
    ZIO.attemptBlocking(
      jc.xrevrange(s"dialog:$dialogId", "+", "-", 50).asScala.toList
    )

  override def close(): Unit = jc.close()

object RedisClientJedis:
  private def createJedisClient(redisConfig: RedisCommonConfig): UnifiedJedis =

    val jedisNodes = redisConfig.servers
      .split(",")
      .map(hostAndPort => hostAndPort.split(":"))
      .map(arr => new HostAndPort(arr(0), arr(1).toInt))
      .toSet

    val jedisClientConfig: JedisClientConfig =
      if redisConfig.password.nonEmpty then
        DefaultJedisClientConfig
          .builder()
          .password(redisConfig.password)
          .build()
      else DefaultJedisClientConfig.builder().build()

    redisConfig.mode match
      case "cluster" =>
        RedisClusterClient
          .builder()
          .nodes(jedisNodes.asJava)
          .clientConfig(jedisClientConfig)
          .build()
      case "sentinel" =>
        RedisSentinelClient
          .builder()
          .sentinels(jedisNodes.asJava)
          .masterName(redisConfig.masterName)
          .clientConfig(jedisClientConfig)
          .sentinelClientConfig(jedisClientConfig)
          .build()
      case "standalone" =>
        JedisRedisClient
          .builder()
          .hostAndPort(jedisNodes.head)
          .clientConfig(jedisClientConfig)
          .build()

  val layer: RLayer[RedisCommonConfig, RedisClient] = ZLayer.scoped(
    for
      conf <- ZIO.service[RedisCommonConfig]
      rc <- ZIO.fromAutoCloseable(
        ZIO.succeed(RedisClientJedis(createJedisClient(conf)))
      )
    yield rc
  )
