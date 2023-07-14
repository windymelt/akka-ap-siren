package com.github.windymelt.apsiren

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.event.Logging
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.ErrorInfo
import akka.http.scaladsl.model.HttpCharsets
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.MediaRange
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.{DateTime => AkkaDateTime}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatcher1
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.StandardRoute
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import com.github.windymelt.apsiren.FollowersRegistry._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Failure
import scala.util.Success

class Routes(
    followersRegistry: ActorRef[FollowersRegistry.Command],
    actorResolverActor: ActorRef[ActorResolver.Command],
    notesRegistry: ActorRef[NotesRegistry.Command],
    publisherActor: ActorRef[Publisher.Command]
)(implicit
    val system: ActorSystem[_]
) {

  import FailFastCirceSupport._

  private val domain: String =
    system.settings.config.getString("sierrapub.domain")

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(
    system.settings.config.getDuration("sierrapub.routes.ask-timeout")
  )

  def follow(url: String, inbox: String): Future[Ok.type] =
    followersRegistry.ask(FollowersRegistry.Add(Follower(url, inbox), _))
  def unfollow(url: String): Future[Ok.type] =
    followersRegistry.ask(FollowersRegistry.Remove(url, _))

  val note = model.Note(
    id = "https://siren.capslock.dev/items/20230116-064829.note.json",
    url = "https://siren.capslock.dev/items/20230116-064829.note.json",
    published = DateTime.parse("2023-01-16T06:48:29Z"),
    to = Seq(
      "https://siren.capslock.dev/followers",
      "https://www.w3.org/ns/activitystreams#Public"
    ),
    attributedTo = "https://siren.capslock.dev/actor",
    content = "ウゥーーーーーーーーーー"
  )
  val note2 = model.Note(
    id = "https://siren.capslock.dev/items/20230708.note.json",
    url = "https://siren.capslock.dev/items/20230708.note.json",
    published = DateTime.parse("2023-07-08T09:17:00Z"),
    to = Seq(
      "https://siren.capslock.dev/followers",
      "https://www.w3.org/ns/activitystreams#Public"
    ),
    attributedTo = "https://siren.capslock.dev/actor",
    content = "リファクタしました"
  )

  val userRoutes: Route =
    route.Actor.route ~ route.Post.route(
      followersRegistry,
      notesRegistry,
      publisherActor
    ) ~ path("inbox") {
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
    } ~ path("outbox") {
      get {
        logRequestResult(("outbox", Logging.InfoLevel)) {
          complete {
            import io.circe.syntax._

            // TODO: Recover some recent activities

            val items = Seq(
              model.Create(
                id =
                  "https://siren.capslock.dev/post/activities/act-yyyy-mm-dd2.create.json",
                url =
                  "https://siren.capslock.dev/post/activities/act-yyyy-mm-dd2.create.json",
                published = DateTime.parse("2023-07-08T09:17:00Z"),
                to = Seq(
                  "https://siren.capslock.dev/followers",
                  "https://www.w3.org/ns/activitystreams#Public"
                ),
                actor = "https://siren.capslock.dev/actor",
                `object` = note2
              ),
              model.Create(
                id =
                  "https://siren.capslock.dev/post/activities/act-yyyy-mm-dd.create.json",
                url =
                  "https://siren.capslock.dev/post/activities/act-yyyy-mm-dd.create.json",
                published = DateTime.parse("2023-01-16T06:48:29Z"),
                to = Seq(
                  "https://siren.capslock.dev/followers",
                  "https://www.w3.org/ns/activitystreams#Public"
                ),
                actor = "https://siren.capslock.dev/actor",
                `object` = note
              )
            )
            val outbox = model.Outbox(
              summary = "outbox of siren",
              totalItems = 2,
              orderedItems = items
            )

            HttpResponse(entity = http.common.activity(outbox))
          }
        }
      }
    } ~ pathPrefix("notes") {
      path(Routes.MatchBase64StrippedUUID) { uuid =>
        get {
          implicit val ec = this.system.executionContext

          complete {
            val note = notesRegistry.ask(NotesRegistry.Get(uuid, _))
            note.map {
              case Some(note) =>
                HttpResponse(entity = http.common.activity(note))
              case None => HttpResponse(StatusCodes.NotFound)
            }
          }
        }
      }
    } ~ pathPrefix("items") {
      concat(
        path("20230116-064829.note.json") {
          get {
            logRequestResult(("item", Logging.InfoLevel)) {
              complete {
                HttpResponse(entity = http.common.activity(note))
              }
            }
          }
        },
        path("20230708.note.json") {
          get {
            logRequestResult(("item", Logging.InfoLevel)) {
              complete {
                HttpResponse(entity = http.common.activity(note2))
              }
            }
          }
        }
      )
    } ~ pathPrefix(".well-known") {
      concat(
        path("webfinger") {
          // TODO: accept only "@siren"
          logRequestResult(("webfinger", Logging.InfoLevel)) {
            get {
              parameter("resource".as[String]) { r =>
                r match {
                  case "acct:siren@siren.capslock.dev" =>
                    // XXX: なんて酷いコードなんだ! これではハエの集会場だ
                    // TODO: まともなコードに直す
                    import io.circe.generic.auto._
                    import io.circe.syntax._
                    val Right(jrd) =
                      ContentType.parse("application/jrd+json; charset=utf-8")
                    val wf =
                      model
                        .Webfinger(
                          subject = r,
                          links = Seq(
                            model.Webfinger.WebfingerLink(
                              rel = "self",
                              `type` = "application/activity+json",
                              href = "https://siren.capslock.dev/actor"
                            )
                          )
                        )
                        .asJson
                        .noSpaces
                        .getBytes()
                    val res = HttpResponse(entity = HttpEntity(jrd, wf))
                    complete(res)
                  case "acct:@siren.capslock.dev" =>
                    // Mastodonがここに何故かアクセスすることがある。404する
                    complete(HttpResponse(StatusCodes.NotFound))
                  case _ => reject
                }
              }
            }
          }
        },
        path("nodeinfo") {
          import io.circe.generic.auto._
          complete(
            model.NodeInfoTop(
              Seq(
                model.NodeInfoTop.Link(
                  "http://nodeinfo.diaspora.software/ns/schema/2.1",
                  "https://siren.capslock.dev/nodeinfo/2.1"
                )
              )
            )
          )
        }
//         path("host-meta") {
//           logRequestResult(("host-meta", Logging.InfoLevel)) {
//             get {
//               complete {
//                 val xml = """<?xml version="1.0"?>
// <XRD xmlns="http://docs.oasis-open.org/ns/xri/xrd-1.0">
//     <Link rel="lrdd" type="application/xrd+xml" template="https://siren.capslock.dev/.well-known/webfinger?resource={uri}" />
// </XRD>"""
//                 val Right(xmltype) =
//                   ContentType.parse("application/xml; charset=UTF-8")
//                 HttpResponse(entity =
//                   HttpEntity(xmltype.asInstanceOf[ContentType.WithCharset], xml)
//                 )
//               }
//             }
//           }
//         }
      )
    } ~ path("nodeinfo" / "2.1") { // TODO: version from build.sbt, name from build.sbt
      get {
        logRequest("nodeinfo21") {
          import io.circe.generic.auto._
          complete(model.NodeInfo())
        }
      }
    }

  // handlers
  // TODO: move
  def handleFollow(json: io.circe.Json): StandardRoute = {
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
            .ask(ActorResolver.ResolveInbox(follower, _))
            .map {
              case Right(followerInbox) =>
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
              case Left(e) =>
                system.log.warn(s"failed to resolve inbox: $e")
                HttpResponse(status = StatusCodes.InternalServerError)
            }
        }
      case _ => reject // nop
    }
  }
  def handleUndo(json: io.circe.Json): StandardRoute = {
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
              this.actorResolverActor
                .ask(ActorResolver.ResolveInbox(soCalledActor, _))
                .map {
                  case Right(followerInbox) =>
                    unfollow(soCalledActor).foreach { _ =>
                      val acceptActivity = model.Accept(
                        s"$domain/accept/${UUID.generate().base64Stripped}",
                        s"$domain/actor",
                        json
                      )
                      val unsignedRequest = HttpRequest(
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
                  case Left(e) =>
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

object Routes {
  val MatchBase64StrippedUUID: PathMatcher1[UUID] = RemainingPath.flatMap {
    path =>
      UUID.fromBase64Stripped(path.toString())
  }
}
