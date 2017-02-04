/*
 *  VideoSet.scala
 *  (Imperfect Reconstruction)
 *
 *  Copyright (c) 2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v2+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.imperfect.raspiplayer

import de.sciss.kollflitz.Vec

import scala.util.Random

object VideoSet {
  /** All but 'fragments'. */
  final val all: Vec[VideoSet] = Vector(VideoSetSite, VideoSetNotebook, VideoSetPrecious, VideoSetMoor)
}
trait VideoSet {
  def select()(implicit random: Random): Vec[Play]
}
