package com.github.windymelt.apsiren
package impl

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.actor.typed.SupervisorStrategy
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import protocol.Followers._

object FollowersComponent {
  def followersBehavior(): Behavior[Command] =
    Behaviors
      .supervise(registry(Set.empty))
      .onFailure(
        SupervisorStrategy.restart
      ) // Always restart (replay) on failure

  private def registry(users: Set[Follower]): Behavior[Command] =
    EventSourcedBehavior[Command, Event, Set[Follower]](
      persistenceId = PersistenceId.ofUniqueId("FollowersRegistry"),
      emptyState = users,
      eventHandler = eventHandler,
      commandHandler = commandHandler
    )

  // Handles Command from other actor. Handling command may result Event.
  // Event will be handled in eventHandler.
  private val commandHandler
      : (Set[Follower], Command) => Effect[Event, Set[Follower]] = {
    case (s: Set[Follower], Add(follower, replyTo)) =>
      s.contains(follower) match {
        case true => // already registered on shared inbox?
          Effect.unhandled.thenRun(_ => replyTo ! Ok)
        case false =>
          Effect.persist(Added(follower)).thenRun(_ => replyTo ! Ok)
      }

    case (s: Set[Follower], Remove(follower, replyTo)) =>
      s.exists(_.url == follower) match {
        case true =>
          // FIX: if we use shared inbox, shared user's remove makes messy
          Effect.persist(Removed(follower)).thenRun(_ => replyTo ! Ok)
        case false =>
          Effect.unhandled.thenRun(_ => replyTo ! Ok)
      }

    case (s: Set[Follower], GetAll(replyTo)) =>
      Effect.none.thenRun(_ => replyTo ! Followers(s))

    case (s: Set[Follower], GetCount(replyTo)) =>
      Effect.none.thenRun(s => replyTo ! s.size)
  }

  // Handles Event. Events are persistent and permanent.
  // When we restart system, Events will replayed and state will be recovered.
  private val eventHandler: (Set[Follower], Event) => Set[Follower] = {
    case followers -> Added(follower) =>
      followers + follower

    case followers -> Removed(followerUrl) =>
      followers.filterNot(_.url == followerUrl)
  }
}
