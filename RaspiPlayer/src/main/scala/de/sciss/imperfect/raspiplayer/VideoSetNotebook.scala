/*
 *  VideoSetNotebook.scala
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

object VideoSetNotebook extends VideoSet {
  private[this] final val durations = Vector[Int](
    4 minutes 11,
    4 minutes 23,
    4 minutes  7,
    4 minutes 12,
    4 minutes 11,
    4 minutes 19,
    4 minutes  7,
    4 minutes 49
  )

  private[this] final val minDur: Int = durations.min
  private[this] final val maxDur: Int = durations.max

  private[this] final val nameFmt = "notebook/notebook%d.mp4"

  def select()(implicit random: Random): Vec[Play] = {
    import Util._

    val durTotI: Int = maxDur // rrand(minDur, maxDur)
    val indices   = random.shuffle(IndicesIn)
    val cmd       = indices.map { i =>
      val file    = nameFmt.format(i + 1)
      val durIn   = durations(i)
      val dur     = math.min(durIn, durTotI)
      val start   = if (durIn <= dur    ) 0f else rrand(0.0, durIn   - dur  ).toFloat
      val delay   = if (durIn >= durTotI) 0f else rrand(0.0, durTotI - durIn).toFloat
      val fadeIn  = math.min(dur * 0.5, rrand(3.0, 4.5)).toFloat
      val fadeOut = math.min(dur * 0.5, rrand(3.0, 4.5)).toFloat
      val screen  = Screen(i)
      val orient  = choose(screen.orientations)
      Play(file = file, delay = delay, start = start, duration = dur, fadeIn = fadeIn, fadeOut = fadeOut,
        orientation = orient)
    }

    cmd
  }
}