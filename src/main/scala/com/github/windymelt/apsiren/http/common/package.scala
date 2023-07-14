package com.github.windymelt.apsiren.http

import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpCharsets
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.Credentials.Missing
import akka.http.scaladsl.server.directives.Credentials.Provided

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
}
