package com.github.windymelt.apsiren
package repo

import akka.actor.typed.ActorRef

trait ActorResolverComponent {
  val actorResolverActor: ActorRef[protocol.ActorResolver.Command]
}
