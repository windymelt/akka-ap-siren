package com.github.windymelt.apsiren.model

/** Create activity defined in ActivityStreams.
  */
case class Create(
    id: String,
    url: String,
    published: String /*TODDO: use DateTime*/,
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
}
