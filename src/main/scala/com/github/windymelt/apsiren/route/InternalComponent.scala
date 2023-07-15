package com.github.windymelt.apsiren
package route

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import http.common.checkBearerToken
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

trait InternalComponent {
  self: ActorSystemComponent with repo.FollowersComponent =>

  implicit private val timeout: Timeout =
    Timeout.durationToTimeout(FiniteDuration(1, "second"))

  val internalRoute: Route =
    pathPrefix("internal") {
      path("followers") {
        get {
          authenticateOAuth2(
            "internal",
            checkBearerToken(
              system.settings.config.getString("sierrapub.post.bearer")
            )
          ) { _ =>
            onSuccess(followersRepository.ask(protocol.Followers.GetAll(_))) {
              followers =>
                import io.circe.generic.auto._
                complete(followers)
            }
          }
        }
      }
    }
}
