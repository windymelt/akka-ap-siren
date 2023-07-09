package com.github.windymelt.apsiren.http

import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.HttpCharsets
import akka.http.scaladsl.model.MediaType

package object common {
  val activityCT = ContentType.WithFixedCharset(
    MediaType.applicationWithFixedCharset(
      "activity+json",
      HttpCharsets.`UTF-8`
    )
  )
}
