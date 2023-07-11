package com.github.windymelt.apsiren

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpRequest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SignatureSpec extends AnyWordSpec with Matchers {
  "HttpSignature" should {
    "sign" in {
      val HttpHeader.ParsingResult.Ok(dateHeader, _) =
        HttpHeader.parse("Date", "Thu, 05 Jan 2012 21:31:40 GMT")
      // val hostHeader =
      //   akka.http.scaladsl.model.headers.Host("example.com")

      implicit val system =
        ActorSystem[Nothing](Behaviors.empty, "HelloAkkaHttpServer")
      val sign = HttpSignature.sign(
        HttpRequest(
          method = akka.http.scaladsl.model.HttpMethods.POST,
          uri = "https://example.com/",
          headers = Seq(dateHeader),
          entity = HttpEntity(
            akka.http.scaladsl.model.ContentTypes.`application/json`,
            "{}"
          )
        )
      )
      val signatureHeader = sign.getHeader("Signature").get.value()
      signatureHeader shouldEqual """keyId="https://siren.capslock.dev/actor",algorithm="rsa-sha256",headers="(request-target) host date digest",signature="WoCC2uSCsMPX7p0En+iMDRSZczzeYnY+Da/W7uxRFbilS3Ro39MI3mmDoRbdCe4VfwH4gfq/V2KUCJa7XXSDb3Y7ZQ5V9uJ5inR6MBSukIFNhk6HNV2FSxxy5ZeaVkDQwXbbDWnESWzD2tSZJ5N0sbD3fVix/u3Ds1c4dkKIt6njb4HVLvOd4G6d2/9ddf+etpKcY8M9JjSehAMEswG1riZmxlSj2kNf2FtR6su9a2RpwFJHYpxY7ZbG3n3sA4u2WqjqsCAdw0GQiKaWm9T5h4Ur15c9pbaqWTvjR1MXsYPg5p2FmaX8mA/AyThPUjB+jRptC3oUiFDcwNiyRTSQlQ==""""
    }
  }
}
