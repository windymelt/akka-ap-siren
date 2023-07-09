package com.github.windymelt.apsiren.model

case class Accept(
    id: String,
    actor: String,
    `object`: io.circe.Json /* 本当はちゃんとしたオブジェクトにしたほうがよさそうだが、やることはオウム返しなのでこれでいい */
)

object Accept {
  import io.circe._, io.circe.generic.semiauto._

  private val AcceptEncoder: Encoder[Accept] = deriveEncoder
  implicit val QualifiedAcceptEncoder: Encoder[Accept] = AcceptEncoder.mapJson {
    j =>
      j.deepMerge(common.activityStreamsHeader)
        .deepMerge(common.typeHeader("Accept"))
  }
}
