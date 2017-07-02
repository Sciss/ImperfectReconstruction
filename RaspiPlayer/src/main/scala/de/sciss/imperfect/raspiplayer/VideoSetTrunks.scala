/*
 *  VideoSetTrunks.scala
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

import de.sciss.kollflitz.Vec

import scala.util.Random

object VideoSetTrunks extends VideoSet {
  private[this] final val nameFmt = "trunks/peripheries-trunk%d.mp4"

  // true = we advance to the right; false = we advance to the left
  private[this] final val directions: Vec[Boolean] = {
    //     1     2     3      4      5     6      7      8
    Vector(true, true, false, false, true, false, false, false)
  }

  def select()(implicit random: Random, screens: Screens): Vec[Play] = {
    import Util._

    val indices   = random.shuffle(IndicesIn)
    val numVid    = directions.size
    val orientIn  = choose(OrientIn)
    val cmd       = indices.map { vidIdx =>
      val idx0    = vidIdx % numVid
      val fIdx    = idx0 + 1
      val file    = nameFmt.format(fIdx)
      val dur     = 120f
      val start   = 0f
      val delay   = 0f
      val fadeIn  = 2f // math.min(dur * 0.5, rrand(3.0, 4.5)).toFloat
      val fadeOut = 2f // math.min(dur * 0.5, rrand(3.0, 4.5)).toFloat
      // make sure all videos are aligned in this set
      val orient  = if (directions(idx0)) orientIn else (orientIn + 180) % 360
      Play(file = file, delay = delay, start = start, duration = dur, fadeIn = fadeIn, fadeOut = fadeOut,
        orientation = orient)
    }

    cmd
  }
}