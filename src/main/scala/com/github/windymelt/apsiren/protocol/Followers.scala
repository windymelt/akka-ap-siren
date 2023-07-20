package com.github.windymelt.apsiren.protocol

import akka.actor.typed.ActorRef

object Followers {
  // TODO: move to domain model
  final case class Follower(
      url: String,
      inbox: String /* TODO: fill fields later */
  )
  sealed trait Command extends CirceAkkaSerializable

  final case class Add(user: Follower, replyTo: ActorRef[Ok.type])
      extends Command

  final case class Remove(url: String, replyTo: ActorRef[Ok.type])
      extends Command

  final case class GetAll(replyTo: ActorRef[Followers]) extends Command
  final case class GetCount(replyTo: ActorRef[Int]) extends Command

  sealed trait Result extends CirceAkkaSerializable
  final case object Ok extends Result
  // いずれインスタンスごとに効率的な配送ができるような構造にする
  final case class Followers(followers: Iterable[Follower]) extends Result

  // actor event(to be persisted)
  sealed trait Event extends CirceAkkaSerializable
  final case class Added(user: Follower) extends Event
  final case class Removed(user: String) extends Event
}
