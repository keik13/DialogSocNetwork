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

import scala.io.Source
import scala.jdk.CollectionConverters.{
  ListHasAsScala,
  MapHasAsJava,
  SetHasAsJava,
  SeqHasAsJava
}

case class RedisClientJedis(jc: UnifiedJedis) extends RedisClient:

  override def xAdd(
      dialogId: String,
      message: Map[String, String]
  ): Task[Unit] =
    ZIO.attemptBlocking(
      jc.xadd(s"dialog:$dialogId", StreamEntryID.NEW_ENTRY, message.asJava)
    )

  override def hSet(
      dialogId: String,
      message: Map[String, String]
  ): Task[Unit] =
    ZIO.attemptBlocking(
      jc.hset(s"dialog:$dialogId:last_read", message.asJava)
    )

  override def hGet(dialogId: String, userId: String): Task[Option[String]] =
    ZIO.attemptBlocking(
      Option(jc.hget(s"dialog:$dialogId:last_read", userId))
    )

  override def xRevRange(dialogId: String): Task[List[StreamEntry]] =
    ZIO.attemptBlocking(
      jc.xrevrange(s"dialog:$dialogId", "+", "-", 50).asScala.toList
    )

  override def xRange(
      dialogId: String,
      oldMsgId: String,
      newMsgId: String
  ): Task[List[StreamEntry]] =
    ZIO.attemptBlocking(
      jc.xrange(s"dialog:$dialogId", oldMsgId, newMsgId).asScala.toList
    )

  def xAddFCall(
      dialogId: String,
      message: Map[String, String]
  ): Task[Unit] = fcall("send_message", dialogId :: message.values.toList).unit

  def xRevRangeFCall(dialogId: String): Task[List[List[String]]] =
    fcall("get_messages", List(dialogId, 50.toString))
      .map(r =>
        r.asInstanceOf[java.util.List[java.util.List[Object]]]
          .asScala
          .toList
          .map(r =>
            r.getLast.asInstanceOf[java.util.List[String]].asScala.toList
          )
      )

  private def fcall(func: String, args: List[String]): Task[Any] =
    ZIO.attemptBlocking(
      jc.fcall(func, Nil.asJava, args.asJava)
    )

  override def functionLoadReplace(): Task[String] =
    for
      script <- readResourceSafe("dialog.lua")
      libName <- ZIO.attemptBlocking(jc.functionLoadReplace(script))
    yield libName

  private def readResourceSafe(fileName: String): Task[String] =
    ZIO.acquireReleaseWith(
      ZIO.attempt(getClass.getClassLoader.getResourceAsStream(fileName))
    )(is => ZIO.succeed(if is != null then is.close())) { is =>
      ZIO.attempt {
        if is == null then throw new Exception(s"Файл $fileName не найден")
        Source.fromInputStream(is).mkString
      }
    }

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
