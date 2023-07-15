package com.github.windymelt.apsiren.protocol

import akka.actor.typed.ActorRef

object ActorResolver {
  final case class Inbox(url: String)
  // actor protocol
  sealed trait Command

  /** Retrieve INBOX of specified actor. Prefers sharedInbox.
    *
    * @param actor
    *   Actor URL
    * @param replyTo
    *   ActorRef to respond with result
    */
  final case class ResolveInbox(
      actor: String,
      replyTo: ActorRef[Either[String, Inbox]]
  ) extends Command
}
