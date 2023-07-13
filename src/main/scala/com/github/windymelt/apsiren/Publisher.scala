package com.github.windymelt.apsiren

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._

object Publisher {
  // actor protocol
  sealed trait Command
  final case class Publish(
      activity: model.Create /* TODO: 他のアクティビティに対応*/,
      inbox: String
  ) extends Command

  def apply(): Behavior[Command] = publisher()

  private def publisher(): Behavior[Command] = Behaviors.receive {
    case ctx -> Publish(activity, inbox) =>
      ctx.log.info(s"Publishing post ${activity.id} -> ${inbox}")
      val unsignedRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = inbox,
        entity = http.common.activityRequest(activity),
        headers = Seq(
          headers.Accept(MediaRange(http.common.activityCT.mediaType)),
          headers.Date(DateTime.now) // akka DateTime
        )
      )

      val signedRequest = HttpSignature.sign(unsignedRequest)(ctx.system)
      Http(ctx.system).singleRequest(signedRequest)

      Behaviors.same
  }
}
