package com.github.windymelt.apsiren.model

case class Undo(id: String, actor: String, `object`: io.circe.Json)

object Undo {
  import io.circe._, io.circe.generic.semiauto._

  private val UndoDecoder: Decoder[Undo] = deriveDecoder
}
