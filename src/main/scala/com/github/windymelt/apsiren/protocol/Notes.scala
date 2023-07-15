package com.github.windymelt.apsiren
package protocol

import akka.actor.typed.ActorRef

object Notes {
  sealed trait Command
  final case class Add(note: model.Note, replyTo: ActorRef[Ok.type])
      extends Command
  final case class Get(id: UUID, replyTo: ActorRef[Option[model.Note]])
      extends Command
  final case class GetRecent(replyTo: ActorRef[Seq[model.Note]]) extends Command

  final case object Ok
}
