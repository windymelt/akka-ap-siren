package com.github.windymelt.apsiren
package impl

import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.MediaRange
import akka.http.scaladsl.model.headers
import protocol.Publisher._

object PublisherComponent {
  def publisherBehavior(): Behavior[Command] =
    Behaviors
      .supervise(publisher())
      .onFailure(SupervisorStrategy.restart)

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
