package com.github.windymelt.apsiren
package route

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.StandardRoute
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

object Post {
  def route(
      followersRegistry: ActorRef[FollowersRegistry.Command],
      notesRegistry: ActorRef[NotesRegistry.Command],
      publisherActor: ActorRef[Publisher.Command]
  )(implicit system: ActorSystem[Nothing]) =
    path("post") {
      // Post endpoint for local client.
      post {
        headerValueByName("Authorization") {
          case s"Bearer $bearer"
              if bearer == system.settings.config.getString(
                "sierrapub.post.bearer"
              ) =>
            entity(as[model.LocalPost]) { post =>
              implicit val timeout: Timeout = Timeout.create(
                system.settings.config.getDuration(
                  "sierrapub.routes.ask-timeout"
                )
              )
              val domain: String =
                system.settings.config.getString("sierrapub.domain")
              implicit val ec = system.executionContext
              // Acquire UUID
              val newUuid = UUID.generate()

              val published = DateTime.now()
              // Transform LocalPost => Note
              val newNote =
                model.Note(
                  id = s"$domain/notes/${newUuid.base64Stripped}",
                  url = s"$domain/notes/${newUuid.base64Stripped}",
                  published = published,
                  to = Seq(
                    s"$domain/followers",
                    "https://www.w3.org/ns/activitystreams#Public"
                  ),
                  attributedTo = s"$domain/actor",
                  content = post.content
                )

              // Wrap Note by Activity
              val activityUuid = UUID.generate()
              val newActivity = model.Create(
                id = s"$domain/activity/${activityUuid.base64Stripped}",
                url = s"$domain/activity/${activityUuid.base64Stripped}",
                published = published,
                to = Seq(
                  s"$domain/followers",
                  "https://www.w3.org/ns/activitystreams#Public"
                ),
                actor = s"$domain/actor",
                `object` = newNote
              )

              // Save Note & Activity
              // TODO: send to Activity Repository
              notesRegistry.ask(NotesRegistry.Add(newNote, _))

              // POST to INBOX
              val followersFuture =
                followersRegistry.ask(FollowersRegistry.GetAll(_))
              followersFuture.foreach { followers =>
                // de-dup inbox.
                // Some instance has own shared inbox.
                // We don't have to send all of users that sharing same inbox.
                val inboxes = followers.followers.map(_.inbox).toSet
                inboxes.foreach { inbox =>
                  publisherActor ! Publisher.Publish(newActivity, inbox)
                }
              }
              complete(
                HttpResponse(
                  status = akka.http.scaladsl.model.StatusCodes.SeeOther,
                  headers = Seq(
                    akka.http.scaladsl.model.headers.Location(Uri(newNote.url))
                  )
                )
              )
            }
          case _ =>
            complete(
              HttpResponse(akka.http.scaladsl.model.StatusCodes.Unauthorized)
            )
        }
      }
    }
}
