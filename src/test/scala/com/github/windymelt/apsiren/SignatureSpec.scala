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
      signatureHeader shouldEqual """keyId="https://siren.capslock.dev/actor#main-key",algorithm="rsa-sha256",headers="(request-target) host date digest",signature="GS/fUJNH9/fFdhUTtmknQA2C8DD8P9IETKAGncloDteqtpVAEqXrHDqObjx2GZQ7b2TBl/cH7ubsuimbY2/QjpnUQdjOpoB5zqquaekOgAJsJSxvCnFqsXbuPllhddcJcPZApBrIurLiYvCLPl9puOEVbcq+nsQJ5NYGA3W0I96jRDdGj3pdN2FtOCvppO9ZqkouAZ5o8XgpFdFbj3ue1FDnrY8kvS/2iDtyFREstCgPW9J6fDvrxmchOOv/WmBl8WUscJdpLgBBwdb+aRiGss6A/eFFLJJygPC2fELRrHKZ1fP+Qu0G2qI1oAgtDXzkzdRun/beih7KtnC+/MMz2A==""""
    }
  }
}
