package com.github.windymelt.apsiren

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior

final case class Follower(
    url: String,
    inbox: String /* TODO: fill fields later */
)

object FollowersRegistry {
  // actor protocol
  sealed trait Command

  final case class Add(user: Follower, replyTo: ActorRef[Ok.type])
      extends Command

  final case class Remove(url: String, replyTo: ActorRef[Ok.type])
      extends Command

  final case class GetAll(replyTo: ActorRef[Followers]) extends Command

  final case object Ok
  // いずれインスタンスごとに効率的な配送ができるような構造にする
  final case class Followers(followers: Iterable[Follower]) extends AnyVal

  // actor event(to be persisted)
  sealed trait Event
  final case class Added(user: Follower) extends Event
  final case class Removed(user: String) extends Event

  def apply(): Behavior[Command] = registry(Set.empty)

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
