package com.github.windymelt.apsiren

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpRequest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SignatureSpec extends AnyWordSpec with Matchers {
  "HttpSignature" should {
    "sign" in {
      val HttpHeader.ParsingResult.Ok(dateHeader, _) =
        HttpHeader.parse("Date", "Thu, 05 Jan 2012 21:31:40 GMT")
      val sign = HttpSignature.sign(
        HttpRequest(uri = "https://example.com/", headers = Seq(dateHeader))
      )
      println(sign.headers)
    }
  }
}
