package com.github.windymelt.apsiren
package route

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

trait NodeInfoComponent {
  import FailFastCirceSupport._

  val nodeInfoRoute =
    path("nodeinfo" / "2.1") { // TODO: version from build.sbt, name from build.sbt
      get {
        logRequest("nodeinfo21") {
          import io.circe.generic.auto._
          complete(model.NodeInfo())
        }
      }
    }
}
