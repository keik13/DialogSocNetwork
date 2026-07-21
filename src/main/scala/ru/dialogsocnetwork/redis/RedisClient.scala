package ru.dialogsocnetwork.redis

import redis.clients.jedis.resps.StreamEntry
import zio.{Chunk, Task}

import java.util.UUID

trait RedisClient extends AutoCloseable:

  def xAdd(dialogId: String, message: Map[String, String]): Task[Unit]

  def hSet(dialogId: String, message: Map[String, String]): Task[Unit]

  def hGet(dialogId: String, userId: String): Task[Option[String]]

  def xRevRange(dialogId: String): Task[List[StreamEntry]]

  def xRange(
      dialogId: String,
      oldMsgId: String,
      newMsgId: String
  ): Task[List[StreamEntry]]

  def functionLoadReplace(): Task[String]

  def xAddFCall(dialogId: String, message: Map[String, String]): Task[Unit]

  def xRevRangeFCall(dialogId: String): Task[List[List[String]]]
