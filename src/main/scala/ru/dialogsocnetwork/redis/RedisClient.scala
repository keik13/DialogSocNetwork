package ru.dialogsocnetwork.redis

import redis.clients.jedis.resps.StreamEntry
import zio.{Chunk, Task}

import java.util.UUID

trait RedisClient extends AutoCloseable:

  def xAdd(dialogId: String, message: Map[String, String]): Task[Unit]

  def xRevRange(dialogId: String): Task[List[StreamEntry]]
