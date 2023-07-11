package com.github.windymelt.apsiren.model

case class LocalPost(content: String)

// Decoder
object LocalPost {
  import io.circe._
  import io.circe.generic.semiauto._
  implicit val LocalPostDecoder: Decoder[LocalPost] = deriveDecoder
}
