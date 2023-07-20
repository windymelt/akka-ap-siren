package com.github.windymelt.apsiren
package model

import com.github.nscala_time.time.Imports._

import model.DateTime._

/** Create activity defined in ActivityStreams.
  */
case class Create(
    id: String,
    url: String,
    published: DateTime,
    to: Seq[String],
    actor: String,
    `object`: Note // TODO: generalize
)

// JSON Encoder/Decoder
object Create {
  import io.circe._, io.circe.generic.semiauto._

  private val CreateEncoder: Encoder[Create] = deriveEncoder
  implicit val QualifiedCreateEncoder: Encoder[Create] = CreateEncoder.mapJson {
    j =>
      j.deepMerge(common.activityStreamsHeader)
        .deepMerge(common.typeHeader("Create"))
  }

  implicit val CreateDecoder: Decoder[Create] = deriveDecoder
}
