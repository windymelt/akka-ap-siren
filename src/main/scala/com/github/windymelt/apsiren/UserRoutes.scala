package com.github.windymelt.apsiren

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.event.Logging
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.ErrorInfo
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.StandardRoute
import akka.util.Timeout
import com.github.windymelt.apsiren.FollowersRegistry._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

//#import-json-formats
//#user-routes-class
class UserRoutes(
    followersRegistry: ActorRef[FollowersRegistry.Command],
    actorResolverActor: ActorRef[ActorResolver.Command]
)(implicit
    val system: ActorSystem[_]
) {

  // #user-routes-class
  import FailFastCirceSupport._
  // #import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(
    system.settings.config.getDuration("my-app.routes.ask-timeout")
  )

  def follow(url: String): Future[Ok.type] =
    followersRegistry.ask(FollowersRegistry.Add(url, _))
  def unfollow(url: String): Future[Ok.type] =
    followersRegistry.ask(FollowersRegistry.Remove(url, _))

  val lf = """
"""
  // #all-routes
  // #users-get-post
  // #users-get-delete
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
              inbox = "https://siren.capslock.dev/inbox",
              outbox = "https://siren.capslock.dev/outbox",
              publicKey = model.ActorPublicKey(
                id = "https://siren.capslock.dev/actor#main-key",
                owner = "https://siren.capslock.dev/actor",
                publicKeyPem = pubkey.replaceAll(lf, "\\\n")
              )
            )

            val bytes = actor.asJson.noSpaces.getBytes()

            val Right(activity) =
              ContentType.parse("application/activity+json; charset=utf-8")

            HttpResponse(entity =
              HttpEntity(activity.asInstanceOf[ContentType.WithCharset], bytes)
            )
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

            val bytes = inbox.asJson.noSpaces.getBytes()

            val Right(activity) =
              ContentType.parse("application/activity+json; charset=utf-8")

            HttpResponse(entity =
              HttpEntity(activity.asInstanceOf[ContentType.WithCharset], bytes)
            )
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

            val note = model.Note(
              id = "https://siren.capslock.dev/items/20230116-064829.note.json",
              url =
                "https://siren.capslock.dev/items/20230116-064829.note.json",
              published =
                "https://siren.capslock.dev/items/20230116-064829.note.json",
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
              published = "https://siren.capslock.dev/items/20230708.note.json",
              to = Seq(
                "https://siren.capslock.dev/followers",
                "https://www.w3.org/ns/activitystreams#Public"
              ),
              attributedTo = "https://siren.capslock.dev/actor",
              content = "リファクタしました"
            )
            val items = Seq(
              model.Create(
                id =
                  "https://siren.capslock.dev/post/activities/act-yyyy-mm-dd2.create.json",
                url =
                  "https://siren.capslock.dev/post/activities/act-yyyy-mm-dd2.create.json",
                published = "2023-07-08T09:17:00Z",
                to = Seq(
                  "http://siren.capslock.dev/followers",
                  "https://www.w3.org/ns/activitystreams#Public"
                ),
                actor = "http://siren.capslock.dev/actor",
                `object` = note2
              ),
              model.Create(
                id =
                  "https://siren.capslock.dev/post/activities/act-yyyy-mm-dd.create.json",
                url =
                  "https://siren.capslock.dev/post/activities/act-yyyy-mm-dd.create.json",
                published = "2023-01-16T06:48:29Z",
                to = Seq(
                  "http://siren.capslock.dev/followers",
                  "https://www.w3.org/ns/activitystreams#Public"
                ),
                actor = "http://siren.capslock.dev/actor",
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
                import io.circe.syntax._
                val note = model.Note(
                  id =
                    "https://siren.capslock.dev/items/20230116-064829.note.json",
                  url =
                    "https://siren.capslock.dev/items/20230116-064829.note.json",
                  published =
                    "https://siren.capslock.dev/items/20230116-064829.note.json",
                  to = Seq(
                    "https://siren.capslock.dev/followers",
                    "https://www.w3.org/ns/activitystreams#Public"
                  ),
                  attributedTo = "https://siren.capslock.dev/actor",
                  content = "ウゥーーーーーーーーーー"
                )

                HttpResponse(entity = activity(note))
              }
            }
          }
        },
        path("20230708.note.json") {
          get {
            logRequestResult(("item", Logging.InfoLevel)) {
              complete {
                import io.circe.syntax._
                val note = model.Note(
                  id = "https://siren.capslock.dev/items/20230708.note.json",
                  url = "https://siren.capslock.dev/items/20230708.note.json",
                  published =
                    "https://siren.capslock.dev/items/20230708.note.json",
                  to = Seq(
                    "https://siren.capslock.dev/followers",
                    "https://www.w3.org/ns/activitystreams#Public"
                  ),
                  attributedTo = "https://siren.capslock.dev/actor",
                  content = "リファクタしました"
                )

                HttpResponse(entity = activity(note))
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
                  case _ => reject
                }
              }
            }
          }
        },
        path("host-meta") {
          logRequestResult(("host-meta", Logging.InfoLevel)) {
            get {
              complete {
                val xml = """<?xml version="1.0"?>
<XRD xmlns="http://docs.oasis-open.org/ns/xri/xrd-1.0">
    <Link rel="lrdd" type="application/xrd+xml" template="https://siren.capslock.dev/.well-known/webfinger?resource={uri}" />
</XRD>"""
                val Right(xmltype) =
                  ContentType.parse("application/xml; charset=UTF-8")
                HttpResponse(entity =
                  HttpEntity(xmltype.asInstanceOf[ContentType.WithCharset], xml)
                )
              }
            }
          }
        }
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

    val Right(activity) =
      ContentType.parse("application/activity+json; charset=utf-8")

    HttpEntity(
      activity.asInstanceOf[ContentType.WithCharset],
      bytes
    )
  }
  def activityRequest[A: io.circe.Encoder](
      j: A
  ): akka.http.scaladsl.model.RequestEntity = {
    import io.circe.syntax._ // for asJson

    val bytes = j.asJson.noSpaces.getBytes()

    val Right(activity) =
      ContentType.parse("application/activity+json; charset=utf-8")

    HttpEntity(
      activity.asInstanceOf[ContentType.WithCharset],
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
    system.log.info("handling follow")
    val targetActor = json.hcursor.get[Option[String]]("actor")
    targetActor match {
      case Right(Some(actor)) =>
        // resolve INBOX
        complete {
          this.actorResolverActor
            .ask(ActorResolver.ResolveInbox(actor, _))
            .map {
              case Right(inbox) =>
                follow(actor).map { _ =>
                  import akka.http.scaladsl.Http
                  import akka.http.scaladsl.model.HttpRequest
                  // Follow list updated. We have to inform to inbox
                  val acceptActivity = model.Accept(actor, json)
                  val acceptEntity = activityRequest(acceptActivity)
                  val unsignedRequest =
                    HttpRequest(
                      uri = inbox.url,
                      entity = acceptEntity,
                      headers =
                        Seq(akka.http.scaladsl.model.headers.Date(DateTime.now))
                    )
                  system.log.info(s"target inbox: $inbox")
                  system.log.info("signing http request")
                  val signedRequest = HttpSignature.sign(unsignedRequest)
                  system.log.info("sending accept")
                  // 投げっぱなし
                  Http()
                    .singleRequest(signedRequest)
                    .onComplete {
                      case Success(res) =>
                        res.discardEntityBytes()
                        system.log.info(s"accept sent: ${res.status}")
                      case Failure(e) => // nop
                        system.log.warn(e.toString())
                    }
                }
                HttpResponse(entity = HttpEntity("ok"))
              case Left(e) =>
                system.log.warn(s"failed to resolve inbox: $e")
                HttpResponse(status = StatusCodes.InternalServerError)
            }
        }
      case _ => reject // nop
    }
  }
}
