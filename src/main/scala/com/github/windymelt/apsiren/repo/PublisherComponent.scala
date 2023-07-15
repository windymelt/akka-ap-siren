package com.github.windymelt.apsiren
package repo

import akka.actor.typed.ActorRef

trait PublisherComponent {
  val publisherActor: ActorRef[protocol.Publisher.Command]
}
