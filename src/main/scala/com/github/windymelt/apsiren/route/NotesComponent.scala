package com.github.windymelt.apsiren
package route

import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import org.fusesource.scalate._

import scala.concurrent.ExecutionContextExecutor
import scala.io.Source

import http.common.accept
import Util.MatchBase64StrippedUUID

trait NotesComponent {
  self: ActorSystemComponent with repo.NotesComponent =>

  implicit val ec: ExecutionContextExecutor = system.executionContext
  val engine = new TemplateEngine()

  val notesRoute: Route = pathPrefix("notes") {
    path(MatchBase64StrippedUUID) { uuid =>
      get {
        accept(MediaTypes.`text/html`) { _ =>
          complete {
            val note = notesRepository.ask(protocol.Notes.Get(uuid, _))
            note.map {
              case Some(note) =>
                val simpleHtml = HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  engine.layout(
                    // resource path. cf. /src/main/resources/
                    "simplenote.mustache",
                    Map[String, Any](
                      "body" -> note.content,
                      "permalink" -> note.url,
                      "by" -> note.attributedTo,
                      "created" -> note.published.toString()
                    )
                  )
                )
                HttpResponse(entity = simpleHtml)
              case None =>
                val errorHtml = HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  engine.layout(
                    "error.mustache",
                    Map[String, Any](
                      "code" -> "404",
                      "message" -> "Not Found."
                    )
                  )
                )
                HttpResponse(StatusCodes.NotFound, entity = errorHtml)
            }
          }
        } ~ {
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
  }

  private implicit val timeout: Timeout = Timeout.create(
    system.settings.config.getDuration("sierrapub.routes.ask-timeout")
  )
}
