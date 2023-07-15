package com.github.windymelt.apsiren

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

trait RoutesComponent {
  self: route.ActorComponent
    with route.PostComponent
    with route.InternalComponent
    with route.InboxComponent
    with route.OutboxComponent
    with route.NotesComponent
    with route.WellKnownComponent
    with route.NodeInfoComponent =>

  // this must be `def` instead of `val`.
  def routes: Route =
    actorRoute ~ postRoute ~ internalRoute ~ inboxRoute ~ outboxRoute ~ notesRoute ~ wellKnownRoute ~ nodeInfoRoute
}
