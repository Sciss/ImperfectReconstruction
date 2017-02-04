/*
 *  VideoSetMoor.scala
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

object VideoSetMoor extends VideoSet {
  private[this] final val durations = Vector[Int](
    4 minutes  9,
    4 minutes  9,
    6 minutes 14,
    6 minutes 14,
    6 minutes 35,
    2 minutes 56, // XXX TODO
    7 minutes 41,
    7 minutes 41
  )

  private[this] final val minDur: Int = durations.min
  private[this] final val maxDur: Int = durations.max

  private[this] final val nameFmt = "moor/moor%d%s.mp4"

  def select()(implicit random: Random): Vec[Play] = {
    import Util._

    val durTotI: Int = math.min(4 minutes 9, rrand(minDur, maxDur))
    val indices   = random.shuffle(IndicesIn)
    val cmd       = indices.zipWithIndex.map { case (ext, screenIdx) =>
      val vidIdx  = ext / 2
      val vr      = if (ext % 2 == 0) "a" else "b"
      val file    = nameFmt.format(vidIdx + 1, vr)
      val durIn   = durations(ext)
      val dur     = math.min(durIn, durTotI)
      val start   = if (durIn <= dur    ) 0f else rrand(0.0, durIn   - dur  ).toFloat
      val delay   = if (durIn >= durTotI) 0f else rrand(0.0, durTotI - durIn).toFloat
      val fadeIn  = math.min(dur * 0.5, rrand(3.0, 4.5)).toFloat
      val fadeOut = math.min(dur * 0.5, rrand(3.0, 4.5)).toFloat
      val screen  = Screen(screenIdx)
      val orient  = choose(screen.orientations)
      Play(file = file, delay = delay, start = start, duration = dur, fadeIn = fadeIn, fadeOut = fadeOut,
        orientation = orient)
    }

    cmd
  }
}