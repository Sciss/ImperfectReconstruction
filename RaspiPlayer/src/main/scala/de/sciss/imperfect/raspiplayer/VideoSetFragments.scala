/*
 *  VideoSetFragments.scala
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

object VideoSetFragments extends VideoSet {
  private[this] final val duration: Int = 18

  private[this] final val indicesExt: Vec[Int] = 0 until 64

  private[this] final val nameFmt  = "fragments/fragment-%d.mp4"
  private[this] final val nameFmtS = "fragments/fragment-s-%d.mp4"

  def select()(implicit random: Random): Vec[Play] = {
    import Util._

    val indices   = random.shuffle(indicesExt).take(IndicesIn.size)
    val cmd       = indices.zipWithIndex.map { case (vidIdx, screenIdx) =>
      val screen  = Screen(screenIdx)
      val fmt     = if (screen.isHanging) nameFmt else nameFmtS
      val file    = fmt.format(vidIdx + 1)
      val fadeIn  = rrand(3.0, 4.5).toFloat
      val fadeOut = rrand(3.0, 4.5).toFloat
      val orient  = choose(screen.orientations)
      Play(file = file, delay = 0f, start = 0f, duration = duration, fadeIn = fadeIn, fadeOut = fadeOut,
        orientation = orient)
    }

    cmd
  }
}