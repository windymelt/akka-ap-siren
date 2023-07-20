package com.github.windymelt.apsiren
package protocol

object Publisher {
  sealed trait Command extends CirceAkkaSerializable
  final case class Publish(
      activity: model.Create /* TODO: 他のアクティビティに対応*/,
      inbox: String
  ) extends Command
}
