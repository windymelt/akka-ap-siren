package com.github.windymelt.apsiren.route

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.StandardRoute
import com.github.windymelt.apsiren.FollowersRegistry
import com.github.windymelt.apsiren.http.common.checkBearerToken
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration
import akka.http.scaladsl.model.HttpEntity
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

object Internal {
  implicit val timeout: Timeout =
    Timeout.durationToTimeout(FiniteDuration(1, "second"))

  def route(followersRegistry: ActorRef[FollowersRegistry.Command])(implicit
      system: ActorSystem[Nothing]
  ) =
    pathPrefix("internal") {
      path("followers") {
        get {
          authenticateOAuth2(
            "internal",
            checkBearerToken(
              system.settings.config.getString("sierrapub.post.bearer")
            )
          ) { _ =>
            onSuccess(followersRegistry.ask(FollowersRegistry.GetAll(_))) {
              followers =>
                import io.circe.generic.auto._
                complete(followers)
            }
          }
        }
      }
    }
}
