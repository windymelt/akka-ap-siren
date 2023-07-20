package com.github.windymelt.apsiren
package route

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.event.Logging
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.actor.typed.scaladsl.AskPattern._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import scala.concurrent.Future
import akka.util.Timeout
import akka.http.scaladsl.server.StandardRoute
import akka.http.scaladsl.model.MediaRange
import scala.util.Success
import scala.util.Failure
import scala.concurrent.duration.FiniteDuration
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.{DateTime => AkkaDateTime}
import protocol.ActorResolver.ActorResolveResult

trait InboxComponent {
  self: ActorSystemComponent
    with repo.FollowersComponent
    with repo.ActorResolverComponent =>

  private implicit val timeout: Timeout = Timeout.create(
    system.settings.config.getDuration("sierrapub.routes.ask-timeout")
  )

  private val domain: String =
    system.settings.config.getString("sierrapub.domain")

  val inboxRoute: Route = path("inbox") {
    get {
      logRequestResult(("inbox", Logging.InfoLevel)) {
        import io.circe.syntax._

        complete {
          val inbox = model.Inbox(
            summary = "inbox of siren",
            totalItems = 0,
            orderedItems = Seq()
          )

          HttpResponse(entity = http.common.activity(inbox))
        }
      }
    } ~ post {
      // follow/remove
      // TODO: Validate HTTP Signature if user want it
      logRequestResult(("inbox-post", Logging.InfoLevel)) {
        mapRequest(http.common.activityAsJson) {
          entity(as[io.circe.Json]) { j =>
            j.hcursor.get[Option[String]]("type") match {
              case Right(Some("Follow")) =>
                system.log.info("inbox received Follow")
                handleFollow(json = j)
              case Right(Some("Undo")) =>
                // TODO: Implement correctly
                system.log.info("inbox received Undo")
                handleUndo(json = j)
              case typ =>
                system.log.warn(s"unimplemented inbox post type: $typ")
                reject // nop
            }
          }
        }
      }
    }
  }

  private def follow(
      url: String,
      inbox: String
  ): Future[protocol.Followers.Ok.type] =
    followersRepository.ask(
      protocol.Followers.Add(protocol.Followers.Follower(url, inbox), _)
    )
  private def unfollow(url: String): Future[protocol.Followers.Ok.type] =
    followersRepository.ask(protocol.Followers.Remove(url, _))

  private def handleFollow(json: io.circe.Json): StandardRoute = {
    implicit val ec = this.system.executionContext
    // follow
    val followerActorTry = json.hcursor.get[Option[String]]("actor")
    // TODO: Validate followee! This endpoint is also sharedInbox.
    system.log.info(s"handling follow: $followerActorTry")
    followerActorTry match {
      case Right(Some(follower)) =>
        // resolve INBOX
        complete {
          this.actorResolverActor
            .ask(protocol.ActorResolver.ResolveInbox(follower, _))
            .map {
              case ActorResolveResult(Right(followerInbox)) =>
                follow(follower, followerInbox.url).map { _ =>
                  import akka.http.scaladsl.Http
                  import akka.http.scaladsl.model.HttpRequest
                  // Follow list updated. We have to inform to inbox
                  val acceptActivity =
                    model.Accept(
                      id =
                        s"https://siren.capslock.dev/accept/${UUID.generate().base64Stripped}",
                      actor = "https://siren.capslock.dev/actor",
                      `object` = json
                    )
                  val unsignedRequest =
                    HttpRequest(
                      method = akka.http.scaladsl.model.HttpMethods.POST,
                      uri = followerInbox.url,
                      entity = http.common.activityRequest(acceptActivity),
                      headers = Seq(
                        akka.http.scaladsl.model.headers
                          .Accept(MediaRange(http.common.activityCT.mediaType)),
                        akka.http.scaladsl.model.headers.Date(AkkaDateTime.now)
                      )
                    )
                  system.log.info(s"Follow accepted: ${acceptActivity.id}")
                  system.log.info(s"target inbox: $followerInbox")
                  system.log.info("signing http request")
                  val signedRequest =
                    HttpSignature.sign(unsignedRequest)(system)
                  system.log.info("sending accept")
                  // 投げっぱなし
                  system.log.info(signedRequest.toString)
                  Http()
                    .singleRequest(signedRequest)
                    .onComplete {
                      case Success(res) =>
                        system.log.info(s"accept sent: ${res.status}")
                        res.entity
                          .toStrict(FiniteDuration(3, "seconds"))
                          .foreach(ent =>
                            system.log
                              .info(new String(ent.data.toArrayUnsafe().clone))
                          )
                      case Failure(e) => // nop
                        system.log.warn(e.toString())
                    }
                }
                HttpResponse(
                  status = StatusCodes.Accepted,
                  entity = HttpEntity.Empty
                )
              case ActorResolveResult(Left(e)) =>
                system.log.warn(s"failed to resolve inbox: $e")
                HttpResponse(status = StatusCodes.InternalServerError)
            }
        }
      case _ => reject // nop
    }
  }
  private def handleUndo(json: io.circe.Json): StandardRoute = {
    import akka.http.scaladsl
    import akka.http.scaladsl.Http
    implicit val ec = this.system.executionContext

    json.hcursor.downField("object").get[String]("type") match {
      case Right("Follow") =>
        val followerToRemove = json.hcursor.get[String]("actor")
        followerToRemove match {
          case Right(soCalledActor) =>
            complete {
              // Actor existence check
              actorResolverActor
                .ask(protocol.ActorResolver.ResolveInbox(soCalledActor, _))
                .map {
                  case ActorResolveResult(Right(followerInbox)) =>
                    unfollow(soCalledActor).foreach { _ =>
                      val acceptActivity = model.Accept(
                        s"$domain/accept/${UUID.generate().base64Stripped}",
                        s"$domain/actor",
                        json
                      )
                      val unsignedRequest = scaladsl.model.HttpRequest(
                        method = scaladsl.model.HttpMethods.POST,
                        uri = followerInbox.url,
                        entity = http.common.activityRequest(acceptActivity),
                        headers = Seq(
                          scaladsl.model.headers
                            .Accept(
                              MediaRange(http.common.activityCT.mediaType)
                            ),
                          scaladsl.model.headers.Date(AkkaDateTime.now)
                        )
                      )
                      system.log.info(
                        s"Unfollow accepted: ${acceptActivity.id}"
                      )
                      val signedRequest =
                        HttpSignature.sign(unsignedRequest)(system)
                      Http().singleRequest(signedRequest)
                    // make unfollow permanent
                    }
                    HttpResponse(StatusCodes.Accepted)
                  case ActorResolveResult(Left(e)) =>
                    system.log.info(s"Actor resolve failed: $e")
                    HttpResponse(StatusCodes.InternalServerError, entity = e)
                }
            }
          case Left(_) => reject
        }
      case Right(_) => reject
      case Left(_)  => reject
    }
  }
}
