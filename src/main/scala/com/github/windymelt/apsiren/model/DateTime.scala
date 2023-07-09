package com.github.windymelt.apsiren.model

import com.github.nscala_time.time.Implicits._
import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormatter

object DateTime {
  import org.joda.time.DateTime
  val JST = +9

  implicit val encodeDateTime: Encoder[DateTime] = new Encoder[DateTime] {
    final def apply(dt: DateTime): Json =
      Json.fromString(
        dt.withZone(DateTimeZone.forOffsetHours(JST)).toString
      )
  }

  implicit val decodeDateTime: Decoder[DateTime] = new Decoder[DateTime] {
    final def apply(s: HCursor): Decoder.Result[DateTime] = {
      ??? // 今回はデコードは使わないので省略
    }
  }
}
