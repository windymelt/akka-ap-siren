package com.github.windymelt.apsiren
package protocol

import akka.actor.ExtendedActorSystem
import org.virtuslab.ash.circe.CirceAkkaSerializer
import org.virtuslab.ash.circe.Register
import org.virtuslab.ash.circe.Registration

class CirceSerializer(actorSystem: ExtendedActorSystem)
    extends CirceAkkaSerializer[CirceAkkaSerializable](actorSystem) {

  override def identifier: Int = 4951344

  import io.circe.generic.auto._

  override lazy val codecs =
    Seq(
      Register[Followers.Command],
      Register[Followers.Event],
      Register[Followers.Result],
      Register[ActorResolver.Command],
      Register[ActorResolver.Result],
      Register[Notes.Command],
      Register[Notes.Result],
      Register[Publisher.Command]
    )

  override lazy val manifestMigrations = Nil

  override lazy val packagePrefix = "com.github.windymelt.apsiren"
}
