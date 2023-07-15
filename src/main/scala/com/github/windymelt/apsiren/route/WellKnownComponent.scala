package com.github.windymelt.apsiren
package route

import akka.event.Logging
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

trait WellKnownComponent {
  import FailFastCirceSupport._

  val wellKnownRoute: Route = pathPrefix(".well-known") {
    concat(
      path("webfinger") {
        // TODO: accept only "@siren"
        logRequestResult(("webfinger", Logging.InfoLevel)) {
          get {
            parameter("resource".as[String]) { r =>
              r match {
                case "acct:siren@siren.capslock.dev" =>
                  // XXX: なんて酷いコードなんだ! これではハエの集会場だ
                  // TODO: まともなコードに直す
                  import io.circe.generic.auto._
                  import io.circe.syntax._
                  val Right(jrd) =
                    ContentType.parse("application/jrd+json; charset=utf-8")
                  val wf =
                    model
                      .Webfinger(
                        subject = r,
                        links = Seq(
                          model.Webfinger.WebfingerLink(
                            rel = "self",
                            `type` = "application/activity+json",
                            href = "https://siren.capslock.dev/actor"
                          )
                        )
                      )
                      .asJson
                      .noSpaces
                      .getBytes()
                  val res = HttpResponse(entity = HttpEntity(jrd, wf))
                  complete(res)
                case "acct:@siren.capslock.dev" =>
                  // Mastodonがここに何故かアクセスすることがある。404する
                  complete(HttpResponse(StatusCodes.NotFound))
                case _ => reject
              }
            }
          }
        }
      },
      path("nodeinfo") {
        import io.circe.generic.auto._
        complete(
          model.NodeInfoTop(
            Seq(
              model.NodeInfoTop.Link(
                "http://nodeinfo.diaspora.software/ns/schema/2.1",
                "https://siren.capslock.dev/nodeinfo/2.1"
              )
            )
          )
        )
      }
//         path("host-meta") {
//           logRequestResult(("host-meta", Logging.InfoLevel)) {
//             get {
//               complete {
//                 val xml = """<?xml version="1.0"?>
// <XRD xmlns="http://docs.oasis-open.org/ns/xri/xrd-1.0">
//     <Link rel="lrdd" type="application/xrd+xml" template="https://siren.capslock.dev/.well-known/webfinger?resource={uri}" />
// </XRD>"""
//                 val Right(xmltype) =
//                   ContentType.parse("application/xml; charset=UTF-8")
//                 HttpResponse(entity =
//                   HttpEntity(xmltype.asInstanceOf[ContentType.WithCharset], xml)
//                 )
//               }
//             }
//           }
//         }
    )
  }
}
