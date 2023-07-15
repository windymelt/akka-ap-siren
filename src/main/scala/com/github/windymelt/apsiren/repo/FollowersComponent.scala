package com.github.windymelt.apsiren
package repo

import akka.actor.typed.ActorRef

trait FollowersComponent {
  val followersRepository: ActorRef[protocol.Followers.Command]
}
