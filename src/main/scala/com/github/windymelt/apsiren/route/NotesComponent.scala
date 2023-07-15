package com.github.windymelt.apsiren
package route

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes

trait NotesComponent {
  self: ActorSystemComponent with repo.NotesComponent =>

  val notesRoute: Route = pathPrefix("notes") {
    path(Routes.MatchBase64StrippedUUID) { uuid =>
      get {
        implicit val ec = system.executionContext

        complete {
          val note = notesRepository.ask(protocol.Notes.Get(uuid, _))
          note.map {
            case Some(note) =>
              HttpResponse(entity = http.common.activity(note))
            case None => HttpResponse(StatusCodes.NotFound)
          }
        }
      }
    }
  }

  private implicit val timeout: Timeout = Timeout.create(
    system.settings.config.getDuration("sierrapub.routes.ask-timeout")
  )
}
