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
import akka.http.scaladsl.model.{DateTime => AkkaDateTime}
import akka.http.scaladsl.server.Directives._
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

class UserRoutes(
    followersRegistry: ActorRef[FollowersRegistry.Command],
    actorResolverActor: ActorRef[ActorResolver.Command]
)(implicit
    val system: ActorSystem[_]
) {

  import FailFastCirceSupport._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(
    system.settings.config.getDuration("my-app.routes.ask-timeout")
  )

  def follow(url: String): Future[Ok.type] =
    followersRegistry.ask(FollowersRegistry.Add(url, _))
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

  val lf = """
"""
  val userRoutes: Route =
    path("actor") {
      logRequestResult(("actor", Logging.InfoLevel)) {
        get {
          complete {
            import io.circe.syntax._

            val actor = model.Actor(
              id = "https://siren.capslock.dev/actor",
              `type` = "Person",
              preferredUserName = Some("siren"),
              name = Some("田舎の昼のサイレンbot"),
              summary = Some(
                "<p>田舎の昼のサイレンbotをActivityPubでも実現しようというハイ・テックなこころみです。</p><p>元bot様とは何ら関係がありません。現在作りかけなので動作していません</p>"
              ),
              inbox = "https://siren.capslock.dev/inbox",
              // Use inbox as sharedInbox because only one actor is active
              sharedInbox = Some("https://siren.capslock.dev/inbox"),
              outbox = "https://siren.capslock.dev/outbox",
              publicKey = model.ActorPublicKey(
                id = "https://siren.capslock.dev/actor#main-key",
                owner = "https://siren.capslock.dev/actor",
                publicKeyPem = pubkey.replaceAll(lf, "\\\n")
              )
            )

            HttpResponse(entity = activity(actor))
          }
        }
      }
    } ~ path("inbox") {
      get {
        logRequestResult(("inbox", Logging.InfoLevel)) {
          import io.circe.syntax._

          complete {
            val inbox = model.Inbox(
              summary = "inbox of siren",
              totalItems = 0,
              orderedItems = Seq()
            )

            HttpResponse(entity = activity(inbox))
          }
        }
      } ~ post {
        // follow/remove
        logRequestResult(("inbox-post", Logging.InfoLevel)) {
          mapRequest(activityAsJson) {
            entity(as[io.circe.Json]) { j =>
              j.hcursor.get[Option[String]]("type") match {
                case Right(Some("Follow")) =>
                  system.log.info("inbox received Follow")
                  handleFollow(json = j)
                case Right(Some("Undo")) =>
                  // TODO: Implement correctly
                  complete("ok")
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

            HttpResponse(entity = activity(outbox))
          }
        }
      }
    } ~ pathPrefix("items") {
      concat(
        path("20230116-064829.note.json") {
          get {
            logRequestResult(("item", Logging.InfoLevel)) {
              complete {
                HttpResponse(entity = activity(note))
              }
            }
          }
        },
        path("20230708.note.json") {
          get {
            logRequestResult(("item", Logging.InfoLevel)) {
              complete {
                HttpResponse(entity = activity(note2))
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

// openssl genrsa -out server.key 2048
// openssl rsa -in server.key -pubout
  val pubkey = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAryP6P3P4X1qNJAIyg9Q4
eAJdzdMBD7oFPqqsyurnWskvCMeljM6sxoohnrVEjD10NZirnyt7X/cpYSc5BMGk
wIfTWyhTMYbNTXlrV0yFrsBtv39tG5TcEWdX1+NvMn68MsCkLv7h/qsz4rBVxmmf
c0lpz9KCqv1AI3mSuJYVNEXP59QuoP0jqtxE2e4Man4hp/BU26XBJJ8i/ZshrXtb
3/3A7K60cYjCboTDwCzD4TYuxgwx0Jgk28zlTYM1NuQNYehpgd5mviUXdFdWatuP
WSuAjGu0T6RNMWTcUh0cV39+wr1fKtZ9rHPubxXz7eikOGvjbB8UIDjXG5Kh7Xv3
WwIDAQAB
-----END PUBLIC KEY-----
"""

  def activity[A: io.circe.Encoder](
      j: A
  ): akka.http.scaladsl.model.ResponseEntity = {
    import io.circe.syntax._ // for asJson

    val bytes = j.asJson.noSpaces.getBytes()

    HttpEntity(
      http.common.activityCT,
      bytes
    )
  }
  def activityRequest[A: io.circe.Encoder](
      j: A
  ): akka.http.scaladsl.model.RequestEntity = {
    import io.circe.syntax._ // for asJson

    println(j.asJson.noSpaces)
    val bytes = j.asJson.noSpaces.getBytes()

    HttpEntity(
      http.common.activityCT,
      bytes
    )
  }
  def activityAsJson(req: HttpRequest): HttpRequest = {
    req.withEntity(req.entity.withContentType(ContentTypes.`application/json`))
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
                follow(follower).map { _ =>
                  import akka.http.scaladsl.Http
                  import akka.http.scaladsl.model.HttpRequest
                  // Follow list updated. We have to inform to inbox
                  val acceptActivity =
                    model.Accept(
                      id =
                        "https://siren.capslock.dev/accept/12345", // TODO: FIXME
                      actor = "https://siren.capslock.dev/actor",
                      `object` = json
                    )
                  val unsignedRequest =
                    HttpRequest(
                      method = akka.http.scaladsl.model.HttpMethods.POST,
                      uri = followerInbox.url,
                      entity = activityRequest(acceptActivity),
                      headers = Seq(
                        akka.http.scaladsl.model.headers
                          .Accept(MediaRange(http.common.activityCT.mediaType)),
                        akka.http.scaladsl.model.headers.Date(AkkaDateTime.now)
                      )
                    )
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
}
