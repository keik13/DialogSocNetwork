package ru.dialogsocnetwork.storage

import zio.Task

import java.util.UUID

trait DialogMessageStorage:

  def add(row: DialogMessageRow): Task[Unit]

  def getDialog(
      dialogUserId: String,
      dialogToUserId: String
  ): Task[List[DialogMessageRow]]
