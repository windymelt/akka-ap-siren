package com.github.windymelt.apsiren

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.event.Logging
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.ErrorInfo
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.StandardRoute
import akka.util.Timeout
import com.github.windymelt.apsiren.UserRegistry._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.concurrent.Future

//#import-json-formats
//#user-routes-class
class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit
    val system: ActorSystem[_]
) {

  // #user-routes-class
  import FailFastCirceSupport._
  // #import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(
    system.settings.config.getDuration("my-app.routes.ask-timeout")
  )

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)
  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))
  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))
  def deleteUser(name: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(name, _))

  val lf = """
"""
  // #all-routes
  // #users-get-post
  // #users-get-delete
  val userRoutes: Route =
    path("actor") {
      logRequestResult(("actor", Logging.InfoLevel)) {
        get { // HARDCODING
          complete(activity(s"""{
	"@context": [
		"https://www.w3.org/ns/activitystreams",
		"https://w3id.org/security/v1"
	],

	"id": "https://siren.capslock.dev/actor",
	"type": "Person",
	"preferredUsername": "siren",
	"inbox": "https://siren.capslock.dev/inbox",
	"outbox": "https://siren.capslock.dev/outbox",

	"publicKey": {
		"id": "https://siren.capslock.dev/actor#main-key",
		"owner": "https://siren.capslock.dev/actor",
		"publicKeyPem": "${pubkey.replaceAll(lf, raw"\\n")}"
	}
}
"""))
        }
      }
    } ~ path("inbox") {
      get { // HARDCODING
        logRequestResult(("inbox", Logging.InfoLevel)) {
          complete(activity("""{
  "@context": "https://www.w3.org/ns/activitystreams",
  "summary": "inbox of siren",
  "type": "OrderedCollection",
  "totalItems": 0,
  "orderedItems": [
  ]
}"""))
        }
      }
    } ~ path("outbox") {
      get {
        logRequestResult(("outbox", Logging.InfoLevel)) {
          complete(activity("""{
  "@context": "https://www.w3.org/ns/activitystreams",
  "summary": "outbox of siren",
  "type": "OrderedCollection",
  "totalItems": 1,
  "orderedItems": [
{
            "@context": "https://www.w3.org/ns/activitystreams",
            "type": "Create",
            "id": "https://siren.capslock.dev/post/activities/act-yyyy-mm-dd.create.json",
            "url": "https://siren.capslock.dev/post/activities/act-yyyy-mm-dd.create.json",
            "published": "2023-01-16T06:48:29Z",
            "to": [
                "http://siren.capslock.dev/followers",
                "https://www.w3.org/ns/activitystreams#Public"
            ],
            "actor": "http://siren.capslock.dev/actor",
            "object": {
                "@context": "https://www.w3.org/ns/activitystreams",
                "type": "Note",
                "id": "https://siren.capslock.dev/items/20230116-064829.note.json",
                "url": "https://siren.capslock.dev/items/20230116-064829.note.json",
                "published": "2023-01-16T06:48:29Z",
                "to": [
                    "https://siren.capslock.dev/followers",
                    "https://www.w3.org/ns/activitystreams#Public"
                ],
                "attributedTo": "https://siren.capslock.dev/actor",
                "content": "ウゥーーーーーーーーーー"
            }
        }
  ]
}"""))
        }
      }
    } ~ path("items" / "20230116-064829.note.json") {
      get {
        logRequestResult(("item", Logging.InfoLevel)) {
          complete(activity("""{
                "@context": "https://www.w3.org/ns/activitystreams",
                "type": "Note",
                "id": "https://siren.capslock.dev/items/20230116-064829.note.json",
                "url": "https://siren.capslock.dev/items/20230116-064829.note.json",
                "published": "2023-01-16T06:48:29Z",
                "to": [
                    "https://siren.capslock.dev/followers",
                    "https://www.w3.org/ns/activitystreams#Public"
                ],
                "attributedTo": "https://siren.capslock.dev/actor",
                "content": "ウゥーーーーーーーーーー"
            }"""))
        }
      }
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
                      Webfinger(
                        subject = r,
                        links = Seq(
                          Webfinger.WebfingerLink(
                            rel = "self",
                            `type` = "application/activity+json",
                            href = "https://siren.capslock.dev/actor"
                          )
                        )
                      ).asJson.noSpaces.getBytes()
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
              complete("""<?xml version="1.0"?>
<XRD xmlns="http://docs.oasis-open.org/ns/xri/xrd-1.0">
    <Link rel="lrdd" type="application/xrd+xml" template="https://siren.capslock.dev/.well-known/webfinger?resource={uri}" />
</XRD>""")
            }
          }
        }
      )
    } ~ path("nodeinfo" / "2.1") { // TODO: version from build.sbt, name from build.sbt
      get {
        logRequest("nodeinfo21") {
          complete(NodeInfo())
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

  // #all-routes
  /** set content type as jrd+json. JRD stands for JSON Resource Descriptor.
    *
    * @param s
    * @return
    */
  def jrd(s: String) = {
    val Right(jrd) = ContentType.parse("application/jrd+json; charset=utf-8")
    HttpEntity(jrd.asInstanceOf[ContentType.WithCharset], s)
  }

  def activity(s: String) = {
    val Right(activity) =
      ContentType.parse("application/activity+json; charset=utf-8")
    HttpEntity(activity.asInstanceOf[ContentType.WithCharset], s)
  }

  def json(s: String) = {
    HttpEntity(ContentTypes.`application/json`, s)
  }
}
