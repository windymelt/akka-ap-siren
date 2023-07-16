package com.github.windymelt.apsiren.http

import akka.http.scaladsl.model
import akka.http.scaladsl.server

import model.ContentType
import model.ContentTypes
import model.HttpCharsets
import model.HttpEntity
import model.HttpRequest
import model.MediaType
import model.headers.Accept
import server.directives._
import server.directives.Credentials.Missing
import server.directives.Credentials.Provided
import server.Directive1
import server.Directives._

package object common {
  val activityCT = ContentType.WithFixedCharset(
    MediaType.applicationWithFixedCharset(
      "activity+json",
      HttpCharsets.`UTF-8`
    )
  )
  def activity[A: io.circe.Encoder](
      j: A
  ): akka.http.scaladsl.model.ResponseEntity = {
    import io.circe.syntax._ // for asJson

    val bytes = j.asJson.noSpaces.getBytes()

    HttpEntity(activityCT, bytes)
  }
  def activityRequest[A: io.circe.Encoder](
      j: A
  ): akka.http.scaladsl.model.RequestEntity = {
    import io.circe.syntax._ // for asJson

    val bytes = j.asJson.noSpaces.getBytes()

    HttpEntity(activityCT, bytes)
  }
  def activityAsJson(req: HttpRequest): HttpRequest = {
    req.withEntity(req.entity.withContentType(ContentTypes.`application/json`))
  }

  /** Authenticator for authenticateOAuth2 directive.
    *
    * @param expectedToken
    *   Bearer token expected.
    * @param c
    *   (internally used)
    * @return
    */
  def checkBearerToken(expectedToken: String)(c: Credentials): Option[Unit] =
    c match {
      case Missing              => None
      case Provided(identifier) => Option.when(identifier == expectedToken)(())
    }

  /** Match when Accept header accepts provided mediaType. Thank you, ChatGPT!
    *
    * @param mediaType
    * @return
    */
  def accept(mediaType: MediaType): Directive1[Accept] =
    headerValueByType[Accept](()).flatMap { accept =>
      if (accept.mediaRanges.exists(_.matches(mediaType))) provide(accept)
      else reject
    }
}
