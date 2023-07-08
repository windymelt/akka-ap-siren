package com.github.windymelt.apsiren.model

case class Outbox(
    summary: String,
    totalItems: Int,
    orderedItems: Seq[Create] /*TODO: generalize*/
)

// JSON Encoder/Decoder
object Outbox {
  import io.circe._, io.circe.generic.semiauto._

  private val OutboxEncoder: Encoder[Outbox] = deriveEncoder
  implicit val QualifiedOutboxEncoder: Encoder[Outbox] =
    OutboxEncoder.mapJson { j =>
      j.deepMerge(common.activityStreamsHeader)
        .deepMerge(common.typeHeader("OrderedCollection"))
    }
}
