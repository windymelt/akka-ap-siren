package com.github.windymelt.apsiren

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.HttpCharset
import akka.http.scaladsl.model.HttpCharsets
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.MediaType
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ContentTypeSpec extends AnyWordSpec with Matchers {
  "ContentType" should {
    "recognize activity+json" in {

      val bytes = "{}".getBytes()

      val activityCT = ContentType.WithFixedCharset(
        MediaType.applicationWithFixedCharset(
          "activity+json",
          HttpCharsets.`UTF-8`
        )
      )

      val ent = HttpEntity(
        activityCT,
        bytes
      )
      println(ent)
    }
  }
}
