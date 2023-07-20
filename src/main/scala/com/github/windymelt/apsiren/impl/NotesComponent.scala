package com.github.windymelt.apsiren
package impl

import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.scaladsl.Behaviors
import protocol.Notes._

object NotesComponent {
  def notesBehavior: Behavior[Command] = Behaviors
    .supervise(registry(Map.empty))
    .onFailure(SupervisorStrategy.restart)

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
        replyTo ! Notes(recent.take(10))
        Behaviors.same
    }
}
