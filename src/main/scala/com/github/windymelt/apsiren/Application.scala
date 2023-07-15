package com.github.windymelt.apsiren

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.Failure
import scala.util.Success

class Application(
    val actorResolverActor: ActorRef[protocol.ActorResolver.Command],
    val followersRepository: ActorRef[protocol.Followers.Command],
    val notesRepository: ActorRef[protocol.Notes.Command],
    val publisherActor: ActorRef[protocol.Publisher.Command]
)(implicit val system: ActorSystem[_])
    extends RoutesComponent
    with ActorSystemComponent
    with repo.ActorResolverComponent
    with repo.FollowersComponent
    with repo.NotesComponent
    with repo.PublisherComponent
    with route.ActorComponent
    with route.PostComponent
    with route.InternalComponent
    with route.InboxComponent
    with route.OutboxComponent
    with route.NotesComponent
    with route.WellKnownComponent
    with route.NodeInfoComponent {}
