package com.github.windymelt.apsiren
package route

import akka.http.scaladsl.server.PathMatcher1
import akka.http.scaladsl.server.Directives.RemainingPath

object Util {
  val MatchBase64StrippedUUID: PathMatcher1[UUID] = RemainingPath.flatMap {
    path =>
      UUID.fromBase64Stripped(path.toString())
  }
}
