package com.github.windymelt.apsiren

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

final case class Follower(url: String /* TODO: fill fields later */ )
final case class Followers(
    users: Seq[Follower]
) // TODO: use accurate data structure

object FollowersRegistry {
  // actor protocol
  sealed trait Command
  final case class Add(url: String, replyTo: ActorRef[Ok.type]) extends Command
  final case class Remove(url: String, replyTo: ActorRef[Ok.type])
      extends Command

  final case object Ok

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(users: Set[Follower]): Behavior[Command] =
    Behaviors.receive {
      case ctx -> Add(u, replyTo) =>
        replyTo ! Ok
        ctx.log.info(s"Follow register: $u")
        registry(users + Follower(u))
      case ctx -> Remove(u, replyTo) =>
        replyTo ! Ok
        ctx.log.info(s"Follow unregister: $u")
        registry(users - Follower(u))
    }
}
