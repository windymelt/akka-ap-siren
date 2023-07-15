package com.github.windymelt.apsiren
package route

import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Failure
import scala.util.Success

import http.common.checkBearerToken

trait InternalComponent {
  self: ActorSystemComponent with repo.FollowersComponent =>

  implicit private val timeout: Timeout =
    Timeout.durationToTimeout(FiniteDuration(30, "second"))

  val internalRoute: Route =
    pathPrefix("internal") {
      path("followers") {
        authenticateOAuth2(
          "internal",
          checkBearerToken(
            system.settings.config.getString("sierrapub.post.bearer")
          )
        ) { _ =>
          get {
            onSuccess(followersRepository.ask(protocol.Followers.GetAll(_))) {
              followers =>
                import io.circe.generic.auto._
                complete(followers)
            }
          } ~ post {
            import io.circe.generic.auto._
            entity(as[protocol.Followers.Followers]) { followers =>
              implicit val ec: ExecutionContext = system.executionContext
              val adds = Future.sequence(
                followers.followers.toSeq
                  .map(f =>
                    followersRepository.ask(protocol.Followers.Add(f, _))
                  )
              )
              onComplete(adds) {
                case Failure(exception) =>
                  complete(500 -> exception.getMessage())
                case Success(value) =>
                  complete("ok")
              }
            }
          }
        }
      }
    }
}
