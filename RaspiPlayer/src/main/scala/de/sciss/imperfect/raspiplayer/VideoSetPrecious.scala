/*
 *  VideoSetPrecious.scala
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

object VideoSetPrecious extends VideoSet {
  private[this] final val durations = Vector[Int](
    2 minutes 52,
    2 minutes 18,
    2 minutes 35,
    2 minutes  0,
    3 minutes 27,
    2 minutes 35,
    3 minutes 10,
    1 minutes 45,
    1 minutes 45
  )

  private[this] final val indicesExt: Vec[Int] = 0 until (durations.size * 4)

  private[this] final val minDur: Int = durations.min
  private[this] final val maxDur: Int = durations.max

  private[this] final val nameFmt = "precious/precious%d%s%s.mp4"

  def select()(implicit random: Random): Vec[Play] = {
    import Util._

    val durTotI: Int = math.min(180, rrand(minDur, maxDur))
    val indices   = random.shuffle(indicesExt).take(IndicesIn.size)
    val cmd       = indices.map { ext =>
      val i       = ext >>> 2
      val bwd     = (ext & 2) != 0
      val variant = (ext & 1) != 0
      val dirS    = if (bwd)      "r" else "f"
      val varS    = if (variant)  "b" else "a"
      val file    = nameFmt.format(i + 1, varS, dirS)
      val durIn   = durations(i)
      val dur     = math.min(durIn, durTotI)
      val start   = if (durIn == dur    ) 0f else rrand(0.0, durIn   - dur  ).toFloat
      val delay   = if (durIn == durTotI) 0f else rrand(0.0, durTotI - durIn).toFloat
      val fadeIn  = math.min(dur * 0.5, rrand(3.0, 4.5)).toFloat
      val fadeOut = math.min(dur * 0.5, rrand(3.0, 4.5)).toFloat
      // val screen  = Screen(i)
      val orient  = choose(OrientIn)
      Play(file = file, delay = delay, start = start, duration = dur, fadeIn = fadeIn, fadeOut = fadeOut,
        orientation = orient)
    }

    cmd
  }
}