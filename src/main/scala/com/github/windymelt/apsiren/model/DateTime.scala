package com.github.windymelt.apsiren.model

import com.github.nscala_time.time.Implicits._
import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormatter
import io.circe.DecodingFailure

object DateTime {
  import org.joda.time.DateTime
  val JST = +9
  val JstZone = DateTimeZone.forOffsetHours(JST)

  implicit val encodeDateTime: Encoder[DateTime] = new Encoder[DateTime] {
    final def apply(dt: DateTime): Json =
      Json.fromString(
        dt.withZone(JstZone).toString
      )
  }

  implicit val decodeDateTime: Decoder[DateTime] = new Decoder[DateTime] {
    import scala.util.control.Exception.allCatch
    final def apply(s: HCursor): Decoder.Result[DateTime] = {
      val str = s.focus.flatMap(_.asString)
      str match {
        case None => Left(DecodingFailure("Not a string", s.history))
        case Some(value) =>
          val parsing = allCatch opt(org.joda.time.DateTime.parse(value))
          parsing match {
            case None => Left(DecodingFailure(s"Could not decode `${value}` as DateTime", s.history))
            case Some(value) => Right(value.withZone(JstZone))
          }
      }
    }
  }
}
