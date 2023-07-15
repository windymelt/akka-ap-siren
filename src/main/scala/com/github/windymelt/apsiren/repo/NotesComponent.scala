package com.github.windymelt.apsiren
package repo

import akka.actor.typed.ActorRef

trait NotesComponent {
  val notesRepository: ActorRef[protocol.Notes.Command]
}
