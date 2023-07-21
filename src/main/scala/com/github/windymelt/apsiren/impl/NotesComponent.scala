package com.github.windymelt.apsiren
package impl

import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.actor.typed.scaladsl.Behaviors
import protocol.Notes._

object NotesComponent {
  def notesBehavior: Behavior[Command] = Behaviors
    .supervise(registry(Map.empty))
    .onFailure(SupervisorStrategy.restart)

  private type State = Map[UUID, model.Note]

  private def registry(map: State): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("NotesRegistry"),
      emptyState = map,
      eventHandler = eventHandler,
      commandHandler = commandHandler
    )
  private val commandHandler: (State, Command) => Effect[Event, State] = {
    case s -> Add(note, replyTo) =>
      /* FIX: domain model separation */
      Effect.persist(Added(note)).thenRun(_ => replyTo ! Ok)
    case s -> Get(uuid, replyTo) =>
      val got = s.get(uuid)
      Effect.none.thenRun(_ => replyTo ! got)
    case s -> GetRecent(replyTo) =>
      // TODO: Use Query-side system.
      // TODO: Multi-actor separation.
      val recent = s.values.toSeq.sortBy(_.published.getMillis().unary_-)
      Effect.none.thenRun(_ => replyTo ! Notes(recent.take(10)))
  }

  private val eventHandler: (State, Event) => State = {
    case s -> Added(note) =>
      val Some(uuid) = UUID.fromBase64Stripped(note.id.takeRight(22))
      s + (uuid -> note)
  }
}
