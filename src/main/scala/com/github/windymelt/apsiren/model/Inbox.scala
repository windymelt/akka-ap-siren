package com.github.windymelt.apsiren.model

case class Inbox(
    summary: String,
    totalItems: Int,
    orderedItems: Seq[Create] /*TODO: generalize*/
)

// JSON Encoder/Decoder
object Inbox {
  import io.circe._, io.circe.generic.semiauto._

  private val InboxEncoder: Encoder[Inbox] = deriveEncoder
  implicit val QualifiedInboxEncoder: Encoder[Inbox] =
    InboxEncoder.mapJson { j =>
      j.deepMerge(common.activityStreamsHeader)
        .deepMerge(common.typeHeader("OrderedCollection"))
    }
}
