package com.github.windymelt.apsiren
package route

import akka.event.Logging
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import http.common._

trait ActorComponent {
  self: ActorSystemComponent =>

  val actorRoute: Route = path("actor") {
    logRequestResult(("actor", Logging.InfoLevel)) {
      get {
        complete {
          import io.circe.syntax._
          val pubkey =
            system.settings.config.getString("sierrapub.server.publicKey")

          val actor = model.Actor(
            id = "https://siren.capslock.dev/actor",
            url = Some("https://siren.capslock.dev/actor"),
            `type` = "Person",
            preferredUsername = Some("siren"),
            name = Some("田舎の昼のサイレンbot"),
            summary = Some(
              "<p>田舎の昼のサイレンbotをActivityPubでも実現しようというハイ・テックなこころみです。</p><p>元bot様とは何ら関係がありません。現在作りかけなので動作がヘンなところがあるかもしれません。</p><p>不具合やお問い合わせは @windymelt@plrm.capslock.dev にどうぞ。</p>"
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
  } ~ pathPrefix("users") {
    path(Util.MatchBase64StrippedUUID) { uuid =>
      get {
        complete("user A/P page")
      }
    }
  }

  val lf = """
"""
}
