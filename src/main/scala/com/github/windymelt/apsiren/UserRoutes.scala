package com.github.windymelt.apsiren

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.github.windymelt.apsiren.UserRegistry._

import scala.concurrent.Future

//#import-json-formats
//#user-routes-class
class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit
    val system: ActorSystem[_]
) {

  // #user-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
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

  // #all-routes
  // #users-get-post
  // #users-get-delete
  val userRoutes: Route =
    pathPrefix("users") {
      concat(
        // #users-get-delete
        pathEnd {
          concat(
            get {
              complete(getUsers())
            },
            post {
              entity(as[User]) { user =>
                onSuccess(createUser(user)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            }
          )
        },
        // #users-get-delete
        // #users-get-post
        path(Segment) { name =>
          concat(
            get {
              // #retrieve-user-info
              rejectEmptyResponse {
                onSuccess(getUser(name)) { response =>
                  complete(response.maybeUser)
                }
              }
              // #retrieve-user-info
            },
            delete {
              // #users-delete-logic
              onSuccess(deleteUser(name)) { performed =>
                complete((StatusCodes.OK, performed))
              }
              // #users-delete-logic
            }
          )
        }
      )
      // #users-get-delete
    } ~ path("actor") {
      get { // HARDCODING
        complete("""{
	"@context": [
		"https://www.w3.org/ns/activitystreams",
		"https://w3id.org/security/v1"
	],

	"id": "https://siren.capslock.dev/actor",
	"type": "Person",
	"preferredUsername": "Siren",
	"inbox": "https://siren.capslock.dev/inbox",

	"publicKey": {
		"id": "https://siren.capslock.dev/actor#main-key",
		"owner": "https://siren.capslock.dev/actor",
		"publicKeyPem": "-----BEGIN PUBLIC KEY-----TODOTODOTODO-----END PUBLIC KEY-----"
	}
}
""")
      }
    } ~ pathPrefix(".well-known") {
      path("webfinger") {
        // TODO: accept only "@siren"
        complete("""{
	"subject": "acct:siren@siren.capslock.dev",

	"links": [
		{
			"rel": "self",
			"type": "application/activity+json",
			"href": "https://siren.capslock.dev/actor"
		}
	]
}
""")
      }
    }
  // #all-routes
}
