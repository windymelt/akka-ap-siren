package com.github.windymelt.apsiren
package route

import akka.event.Logging
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import http.common._

object Actor {
  val route: Route = path("actor") {
    logRequestResult(("actor", Logging.InfoLevel)) {
      get {
        complete {
          import io.circe.syntax._

          val actor = model.Actor(
            id = "https://siren.capslock.dev/actor",
            `type` = "Person",
            preferredUsername = Some("siren"),
            name = Some("田舎の昼のサイレンbot"),
            summary = Some(
              "<p>田舎の昼のサイレンbotをActivityPubでも実現しようというハイ・テックなこころみです。</p><p>元bot様とは何ら関係がありません。現在作りかけなので動作していません</p>"
            ),
            inbox = "https://siren.capslock.dev/inbox",
            // Use inbox as sharedInbox because only one actor is active
            sharedInbox = Some("https://siren.capslock.dev/inbox"),
            outbox = "https://siren.capslock.dev/outbox",
            publicKey = model.ActorPublicKey(
              id = "https://siren.capslock.dev/actor#main-key",
              owner = "https://siren.capslock.dev/actor",
              publicKeyPem = pubkey.replaceAll(lf, "\\\n")
            )
          )

          HttpResponse(entity = activity(actor))
        }
      }
    }
  }
// openssl genrsa -out server.key 2048
// openssl rsa -in server.key -pubout
  val pubkey = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAryP6P3P4X1qNJAIyg9Q4
eAJdzdMBD7oFPqqsyurnWskvCMeljM6sxoohnrVEjD10NZirnyt7X/cpYSc5BMGk
wIfTWyhTMYbNTXlrV0yFrsBtv39tG5TcEWdX1+NvMn68MsCkLv7h/qsz4rBVxmmf
c0lpz9KCqv1AI3mSuJYVNEXP59QuoP0jqtxE2e4Man4hp/BU26XBJJ8i/ZshrXtb
3/3A7K60cYjCboTDwCzD4TYuxgwx0Jgk28zlTYM1NuQNYehpgd5mviUXdFdWatuP
WSuAjGu0T6RNMWTcUh0cV39+wr1fKtZ9rHPubxXz7eikOGvjbB8UIDjXG5Kh7Xv3
WwIDAQAB
-----END PUBLIC KEY-----
"""
  val lf = """
"""
}
