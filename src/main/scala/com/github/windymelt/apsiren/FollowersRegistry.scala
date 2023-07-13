package com.github.windymelt.apsiren

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

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

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(users: Set[Follower]): Behavior[Command] =
    Behaviors.receive {
      case ctx -> Add(u, replyTo) =>
        replyTo ! Ok
        ctx.log.info(s"Follow register: $u")
        registry(users + u)
      case ctx -> Remove(u, replyTo) =>
        replyTo ! Ok
        ctx.log.info(s"Follow unregister: $u")
        registry(users.filterNot(_.url == u))
      case ctx -> GetAll(replyTo) =>
        replyTo ! Followers(users)
        Behaviors.same
    }
}
