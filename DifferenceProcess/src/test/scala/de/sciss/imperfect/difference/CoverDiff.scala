package de.sciss.imperfect.difference

import de.sciss.file._

object CoverDiff {
  def main(args: Array[String]): Unit = {
    val widthInMM     = 413.0 // 407.116
    val heightInMM    = 208.0 // 202.116

    val dpi           = 200
    val widthIn       = widthInMM  / 10 / 2.54
    val widthPixOut0  = (dpi * widthIn ).round.toInt // 3206
    val widthPixOut   = (widthPixOut0 + 1) & ~1
    val heightIn      = heightInMM / 10 / 2.54
    val heightPixOut0 = (dpi * heightIn).round.toInt // 1591
    val heightPixOut  = (heightPixOut0 + 1) & ~1

    val widthPixIn    = 3280
//    val heightPixIn   = 2464

    val trimLeft      = (widthPixIn  - widthPixOut )/2  // 37
    val trimTop       = 380 // (heightPixIn - heightPixOut)/2  // 436
    val seqLen        = 40
    val gain          = 6.0
    val threshold     = 0.2
    val greenGain     = 1.2
    val rotate        = 180
    val autoLevels    = true
    val gamma         = 0.9 // 1.2

    val framesIn      = 5026
    val framesOut     = 500 + 1
//    val frameStep     = 10
    val frameStep     = (framesIn - seqLen) / framesOut

    val baseDir = file("/") /"data" / "projects" / "Imperfect"
    val config = DifferenceProcess.Config(
      templateIn  = baseDir / "site-2" / "frame-%d.jpg",
      templateOut = baseDir / "catalog" / "cover" / "site-2out_catalog" / "frame-%d.jpg",
      idxRange0   = 1 to framesIn, // 1300 to 3704,
      width       = widthPixOut,
      height      = heightPixOut,
      trimLeft    = trimLeft,
      trimTop     = trimTop,
      gain        = gain,
      gamma       = gamma,
      seqLen      = seqLen,
      medianSide  = 3,
      thresh      = threshold,
      redGain     = 1.0,
      greenGain   = greenGain,
      blueGain    = 1.0,
      frameStep   = frameStep,
      rotate      = rotate,
      autoLevels  = autoLevels
    )
    DifferenceProcess.run(config)
  }
}