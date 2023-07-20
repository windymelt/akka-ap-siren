package com.github.windymelt.apsiren
package protocol

import akka.actor.typed.ActorRef

object Notes {
  sealed trait Command extends CirceAkkaSerializable
  final case class Add(note: model.Note, replyTo: ActorRef[Ok.type])
      extends Command
  final case class Get(id: UUID, replyTo: ActorRef[Option[model.Note]])
      extends Command
  final case class GetRecent(replyTo: ActorRef[Notes]) extends Command

  sealed trait Result extends CirceAkkaSerializable
  final case object Ok extends Result
  final case class Notes(notes: Seq[model.Note]) extends Result
}
