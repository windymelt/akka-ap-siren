package com.github.windymelt.apsiren

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.Failure
import scala.util.Success

//#main-class
object App {
  def main(args: Array[String]): Unit = {
    // #server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val followersRepository =
        context.spawn(
          impl.FollowersComponent.followersBehavior,
          "UserRegistryActor"
        )
      context.watch(followersRepository)

      val actorResolverActor =
        context.spawn(
          impl.ActorResolverComponent.actorResolverBehavior,
          "ActorResolverActor"
        )
      context.watch(actorResolverActor)

      val notesRepositoryActor =
        context.spawn(impl.NotesComponent.notesBehavior, "NotesRegistryActor")
      context.watch(notesRepositoryActor)

      val publisherActor =
        context.spawn(
          impl.PublisherComponent.publisherBehavior,
          "PublisherActor"
        )
      context.watch(publisherActor)

      val appliation = new Application(
        actorResolverActor = actorResolverActor,
        followersRepository = followersRepository,
        notesRepository = notesRepositoryActor,
        publisherActor = publisherActor
      )(context.system)

      context.system.log.warn("starting http server...")

      startHttpServer(appliation.routes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "SierraPub")
    // #server-bootstrapping
  }

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

}
//#main-class
