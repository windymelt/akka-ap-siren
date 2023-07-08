package com.github.windymelt.apsiren.model

package object common {
  import io.circe._

  /** JSON header always appeared in ActivityStreams object.
    *
    * To reduce boilerplate, we can reuse this header object using
    * [[io.circe.Json.deepMerge]].
    */
  val activityStreamsHeader =
    Json.obj(
      "@context" -> Json.fromString("https://www.w3.org/ns/activitystreams")
    )

  def typeHeader(`type`: String): Json =
    Json.obj("type" -> Json.fromString(`type`))
}
