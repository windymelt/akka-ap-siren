package com.github.windymelt.apsiren

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object NotesRegistry {
  // actor protocol
  sealed trait Command
  final case class Add(note: model.Note, replyTo: ActorRef[Ok.type])
      extends Command
  final case class Get(id: UUID, replyTo: ActorRef[Option[model.Note]])
      extends Command
  final case class GetRecent(replyTo: ActorRef[Seq[model.Note]]) extends Command

  final case object Ok

  def apply(): Behavior[Command] = registry(Map.empty)

  private def registry(map: Map[UUID, model.Note]): Behavior[Command] =
    Behaviors.receive {
      case ctx -> Add(note, replyTo) =>
        replyTo ! Ok
        /* FIX: domain model separation */
        val Some(uuid) = UUID.fromBase64Stripped(note.id.takeRight(22))
        ctx.log.info(s"Saved note: ${note.url}")
        registry(
          map + (uuid -> note)
        )
      case ctx -> Get(uuid, replyTo) =>
        val got = map.get(uuid)
        replyTo ! got
        if (got.isDefined) {
          ctx.log.info(s"Retrieving note: ${got.get.url}")
        }
        Behaviors.same
      case ctx -> GetRecent(replyTo) =>
        // TODO: Use Query-side system.
        // TODO: Multi-actor separation.
        val recent = map.values.toSeq.sortBy(_.published.getMillis().unary_-)
        replyTo ! recent.take(10)
        Behaviors.same
    }
}
