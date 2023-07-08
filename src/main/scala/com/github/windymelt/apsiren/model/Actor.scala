package com.github.windymelt.apsiren.model

/** Actor object defined in ActivityStreams.
  */
case class Actor(
    id: String,
    preferredUserName: String,
    `type`: String,
    inbox: String,
    outbox: String,
    publicKey: ActorPublicKey
)

case class ActorPublicKey(id: String, owner: String, publicKeyPem: String)

// JSON Encoder/Decoder
object Actor {
  import io.circe._, io.circe.generic.semiauto._

  private implicit val ActorPublicKeyEncoder: Encoder[ActorPublicKey] =
    deriveEncoder
  private val ActorEncoder: Encoder[Actor] = deriveEncoder
  implicit val QualifiedActorEncoder: Encoder[Actor] = ActorEncoder.mapJson {
    j =>
      val context = common.activityStreamsHeader.deepMerge(
        Json.obj(
          "@context" -> Json.arr(
            Json.fromString("https://www.w3.org/ns/activitystreams"),
            Json.fromString("https://w3id.org/security/v1")
          )
        )
      )
      j.deepMerge(context)
  }
}
