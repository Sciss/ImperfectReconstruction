package de.sciss.imperfect.raspiplayer

object Status {
  def apply(id: String): Status = id match {
    case Unknown.id => Unknown
    case Idle   .id => Idle
  }
}
sealed trait Status { def id: String }
case object Unknown extends Status { final val id = "unknown" }
case object Idle    extends Status { final val id = "idle"    }
