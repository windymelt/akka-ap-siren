package com.github.windymelt.apsiren.model

/** Note object defined in ActivityStreams.
  *
  * @param id
  * @param url
  * @param published
  * @param to
  * @param attributedTo
  * @param content
  */
case class Note(
    id: String,
    url: String,
    published: String /*TODO: use DateTime via nscala-time*/,
    to: Seq[String],
    attributedTo: String,
    content: String
) extends ASObject

// JSON Encoder/Decoder
object Note {
  import io.circe._, io.circe.generic.semiauto._

  private val NoteEncoder: Encoder[Note] = deriveEncoder
  implicit val QualifiedNoteEncoder: Encoder[Note] = NoteEncoder.mapJson { j =>
    j.deepMerge(common.activityStreamsHeader)
      .deepMerge(common.typeHeader("Note"))
  }
  private val NoteDecoder: Decoder[Note] = deriveDecoder
  implicit val QualifiedNoteDecoder: Decoder[Note] =
    NoteDecoder // TODO: fix something
}
