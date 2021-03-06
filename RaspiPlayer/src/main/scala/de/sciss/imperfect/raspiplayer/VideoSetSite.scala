/*
 *  VideoSetSite.scala
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

object VideoSetSite extends VideoSet {
  private[this] final val durations = Vector[Int](
    7 minutes 38,
    7 minutes 38,
    9 minutes 45,
    9 minutes 46,
    8 minutes 43,
    8 minutes 43,
    6 minutes 22,
    6 minutes 18
  )

  private[this] final val minDur: Int = durations.min
  private[this] final val maxDur: Int = durations.max

  private[this] final val nameFmt = "site/site%d.mp4"

  def select()(implicit random: Random, screens: Screens): Vec[Play] = {
    import Util._

    val durTotI: Int = rrand(minDur, maxDur)
    val indices   = random.shuffle(IndicesIn)
    val cmd       = indices.zipWithIndex.map { case (vidIdx, screenIdx) =>
      val file    = nameFmt.format(vidIdx + 1)
      val durIn   = durations(vidIdx)
      val dur     = math.min(durIn, durTotI)
      val start   = if (durIn <= dur    ) 0f else rrand(0.0, durIn   - dur  ).toFloat
      val delay   = if (durIn >= durTotI) 0f else rrand(0.0, durTotI - durIn).toFloat
      val fadeIn  = math.min(dur * 0.5, rrand(3.0, 4.5)).toFloat
      val fadeOut = math.min(dur * 0.5, rrand(3.0, 4.5)).toFloat

      val screen  = screens(screenIdx)
      val orient  = choose(screen.orientations)

//      println(s"screenIdx $screenIdx, vidIdx $vidIdx, hanging? ${screen.isHanging}, orientations: ${screen.orientations}")

      Play(file = file, delay = delay, start = start, duration = dur, fadeIn = fadeIn, fadeOut = fadeOut,
        orientation = orient)
    }

    cmd
  }
}
