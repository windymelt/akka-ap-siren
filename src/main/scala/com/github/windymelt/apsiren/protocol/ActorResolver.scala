package com.github.windymelt.apsiren.protocol

import akka.actor.typed.ActorRef

object ActorResolver {
  sealed trait Result extends CirceAkkaSerializable
  final case class ActorResolveResult(result: Either[String, Inbox])
      extends Result
  final case class Inbox(url: String)
  // actor protocol
  sealed trait Command extends CirceAkkaSerializable

  /** Retrieve INBOX of specified actor. Prefers sharedInbox.
    *
    * @param actor
    *   Actor URL
    * @param replyTo
    *   ActorRef to respond with result
    */
  final case class ResolveInbox(
      actor: String,
      replyTo: ActorRef[ActorResolveResult]
  ) extends Command
}
