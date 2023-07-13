package com.github.windymelt.apsiren

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.Failure
import scala.util.Success

//#main-class
object App {
  // #start-http-server
  private def startHttpServer(
      routes: Route
  )(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = Http().newServerAt("0.0.0.0", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "Server online at http://{}:{}/",
          address.getHostString,
          address.getPort
        )
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
  // #start-http-server
  def main(args: Array[String]): Unit = {
    // #server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val followersRegistryActor =
        context.spawn(FollowersRegistry(), "UserRegistryActor")
      context.watch(followersRegistryActor)

      val actorResolverActor =
        context.spawn(ActorResolver(), "ActorResolverActor")
      context.watch(actorResolverActor)

      val notesRepositoryActor =
        context.spawn(NotesRegistry(), "NotesRegistryActor")
      context.watch(notesRepositoryActor)

      val publisherActor = context.spawn(Publisher(), "PublisherActor")
      context.watch(publisherActor)

      val routes = new Routes(
        followersRegistryActor,
        actorResolverActor,
        notesRepositoryActor,
        publisherActor
      )(
        context.system
      )
      startHttpServer(routes.userRoutes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "SierraPub")
    // #server-bootstrapping
  }
}
//#main-class
