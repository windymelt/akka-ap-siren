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
      val hostHeader =
        akka.http.scaladsl.model.headers.Host("siren.capslock.dev")

      implicit val system =
        ActorSystem[Nothing](Behaviors.empty, "HelloAkkaHttpServer")
      val sign = HttpSignature.sign(
        HttpRequest(
          method = akka.http.scaladsl.model.HttpMethods.POST,
          uri = "https://example.com/",
          headers = Seq(dateHeader, hostHeader),
          entity = HttpEntity(
            akka.http.scaladsl.model.ContentTypes.`application/json`,
            "{}"
          )
        )
      )
      val signatureHeader = sign.getHeader("Signature").get.value()
      signatureHeader shouldEqual """keyId="https://siren.capslock.dev/actor#main-key",algorithm="rsa-sha256",headers="(request-target) host date content-digest content-type",signature="csODAm6spYa1cH/mSAiI3LVAtScVSYfv8gJesrA+eACjnbvWqTu0TEufwcGmg2uC7iYbwW3WnQgRmy5vVnoTbacY8y/9ulNH1E6Ip/YjPCMPqKnrzC5aqXfF6cSuQfRBRlzKbEvGFE9TetgX2xS2LlBanGmlQ3MRrSXHSzBnWYfjE/mC2t1qweFSLQQKAK80378O2zt296akgBp+v8QNwoRoxdXSkEaBRHhX5FKTV1bM1iQZ9czckRtSdugg5cBPIrZUAVpdy+irid+Nj/sjO/tdvFI2KyObBZaR/IimwuesiOMFAbAQXqO6qvR2ZRFK1gCTChpR9VjGkF8yuEUwJw==""""
    }
  }
}
