package com.github.windymelt.apsiren

import akka.actor.typed.ActorSystem

trait ActorSystemComponent {
  implicit val system: ActorSystem[Nothing]
}
