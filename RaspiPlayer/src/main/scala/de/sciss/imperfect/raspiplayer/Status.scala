/*
 *  Status.scala
 *  (Imperfect Reconstruction)
 *
 *  Copyright (c) 2016-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v2+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.imperfect.raspiplayer

object Status {
  def apply(id: String): Status = id match {
    case Idle   .id => Idle
    case Playing.id => Playing
    case Unknown.id => Unknown
  }
}
sealed trait Status { def id: String }
case object Idle    extends Status { final val id = "idle"    }
case object Playing extends Status { final val id = "playing" }
case object Unknown extends Status { final val id = "unknown" }