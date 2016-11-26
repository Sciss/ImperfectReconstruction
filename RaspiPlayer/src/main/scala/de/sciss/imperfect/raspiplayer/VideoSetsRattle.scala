package de.sciss.imperfect.raspiplayer

import de.sciss.kollflitz._

import scala.util.Random

object VideoSetsRattle {
  final val all: Vec[VideoSet] = Vector(Neuro, Phase)

  trait Common extends VideoSet {
    protected def durations: Vector[Int]
    protected def minDur: Int
    protected def maxDur: Int
    protected def nameFmt: String

    def select()(implicit random: Random): Vec[Play] = {
      import Util._

      val durTotI: Int = rrand(minDur, maxDur)
      val indices   = random.shuffle(IndicesIn)
      val cmd       = indices.zipWithIndex.map { case (vidIdx, screenIdx) =>
        val file    = nameFmt.format(vidIdx + 1)
        val durIn   = durations(vidIdx)
        val dur     = math.min(durIn, durTotI)
        val start   = if (durIn <= dur    ) 0f else rrand(0.0, durIn   - dur  ).toFloat
        val delay   = if (durIn >= durTotI) 0f else rrand(0.0, durTotI - durIn).toFloat
        val fadeIn  = 1f
        val fadeOut = 1f

        val screen  = Screen(screenIdx)
        val orient  = choose(screen.orientations)

        Play(file = file, delay = delay, start = start, duration = dur, fadeIn = fadeIn, fadeOut = fadeOut,
          orientation = orient)
      }

      cmd
    }
  }

  object Neuro extends Common {
    protected final val durations = Vector[Int](
      1 minutes 42,
      1 minutes 40,
      1 minutes 41,
      1 minutes 50,
      1 minutes 50,
      1 minutes 41,
      1 minutes 50,
      1 minutes 50
    )

    protected final val minDur: Int = durations.min
    protected final val maxDur: Int = durations.max

    protected final val nameFmt = "rattle/neuro_c_%d.mp4"
  }

  object Phase extends Common {
    protected final val durations = Vector[Int](
      2 minutes 36,
      2 minutes  6,
      2 minutes 40,
      2 minutes  5,
      1 minutes 33,
      1 minutes 52,
      1 minutes 41,
      5 minutes 44
    )

    protected final val minDur: Int = durations.min
    protected final val maxDur: Int = durations.max

    protected final val nameFmt = "rattle/phase_c_%d.mp4"
  }
}
