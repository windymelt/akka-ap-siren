package com.github.windymelt.apsiren.model

object MisskeySpecific {

  /** Misskeyではアクターにこのフィールドがないとうまく認識されないようだ
    */
  val discoverableField =
    io.circe.Json.obj("discoverable" -> io.circe.Json.True)
}
