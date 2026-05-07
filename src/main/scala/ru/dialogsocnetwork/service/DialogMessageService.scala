package ru.dialogsocnetwork.service

import ru.dialogsocnetwork.api.{DialogMessage, DialogMessageText}
import zio.Task

import java.util.UUID

trait DialogMessageService:

  def add(
      request: DialogMessageText,
      userId: UUID,
      toUserId: UUID
  ): Task[Unit]
  
  def addF(
      request: DialogMessageText,
      userId: UUID,
      toUserId: UUID
  ): Task[Unit]

  def getById(userId: UUID, toUserId: UUID): Task[List[DialogMessage]]
  
  def getByIdF(userId: UUID, toUserId: UUID): Task[List[DialogMessage]]
