package com.github.windymelt.apsiren
package route

import akka.actor.typed.scaladsl.AskPattern._
import akka.event.Logging
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import protocol.Notes._

trait OutboxComponent {
  self: ActorSystemComponent with repo.NotesComponent =>

  val outboxRoute = path("outbox") {
    get {
      logRequestResult(("outbox", Logging.InfoLevel)) {
        onSuccess(notesRepository.ask(protocol.Notes.GetRecent(_))) {
          case Notes(notes) =>
            complete {
              import io.circe.syntax._

              // TODO: Save activity to correctly recover
              val activities = notes.map { n =>
                val uuid = UUID.generate().base64Stripped
                model.Create(
                  id = s"$domain/activities/$uuid",
                  url = s"$domain/activities/$uuid",
                  published = n.published,
                  to = Seq(
                    "https://siren.capslock.dev/followers",
                    "https://www.w3.org/ns/activitystreams#Public"
                  ),
                  actor = n.attributedTo,
                  `object` = n
                )
              }

              val outbox = model.Outbox(
                summary = "outbox of siren",
                totalItems = activities.size,
                orderedItems = activities
              )

              HttpResponse(entity = http.common.activity(outbox))
            }
        }
      }
    }
  }

  private implicit val timeout: Timeout = Timeout.create(
    system.settings.config.getDuration("sierrapub.routes.ask-timeout")
  )

  private val domain: String =
    system.settings.config.getString("sierrapub.domain")
}
