package com.github.windymelt.apsiren

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.MediaRange
import akka.http.scaladsl.model.headers
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.duration.FiniteDuration
import scala.util.Failure
import scala.util.Success

import model.Actor

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

  def apply(): Behavior[Command] =
    Behaviors.receive { // using .receive to use system and ec
      // Resolving inbox for specific actor.
      // We GET to actor URL and get inbox field on it.
      case (ctx, ResolveInbox(actor, replyTo)) =>
        // make http request to get actor info
        implicit val system = ctx.system
        implicit val ec = ctx.executionContext
        // We don't need signing HTTP request here.
        // Note: Prepending .json to actor URL seems to be Mastodon-specific impl..
        // using Accept header.
        ctx.log.info("resolving actor...")
        Http()
          .singleRequest(
            HttpRequest(
              uri = actor,
              headers = Seq(
                headers.Accept(MediaRange(http.common.activityCT.mediaType))
              )
            )
          )
          .onComplete {
            case Success(resp) =>
              // Request succeed. Because Akka HTTP is stream-oriented, we use .toStrict to convert it to normal response.
              resp.entity.toStrict(FiniteDuration(1, "seconds")).map { entity =>
                val bodyString = entity.data.utf8String
                decode[Actor](bodyString) match {
                  case Right(a: Actor) =>
                    // now we have INBOX.
                    // Prefer shared INBOX
                    val inbox = Inbox(a.sharedInbox.getOrElse(a.inbox))
                    replyTo ! Right(inbox)
                  case Left(e) =>
                    replyTo ! Left(e.toString) // JSON decoding failure
                }
              }
            case Failure(e) => ctx.log.warn(e.toString())
          }

        Behaviors.same
    }
}
