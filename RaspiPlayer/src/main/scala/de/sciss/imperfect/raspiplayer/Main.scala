/*
 *  Main.scala
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

import de.sciss.file.File

object Main {
  def main(args: Array[String]): Unit = {
    val p = new scopt.OptionParser[Config]("Imperfect-RaspiPlayer") {
      opt[File]("test-video")
        .text ("Test video file")
        .required()
        .action { (f, c) => c.copy(testVideo = f) }

//      opt[Int] ('s', "start-frame")
//        .text ("Start frame index")
//        .action   { (v, c) => c.copy(startFrame = v) }
//        .validate {  v     => if (v >= 0) success else failure("start-frame must be >= 0") }

      opt[Unit] ('d', "dump-osc")
        .text ("Enable OSC dump")
        .action   { (v, c) => c.copy(dumpOSC = true) }

      opt[Unit] ('c', "control")
        .text ("Instance is control center")
        .action   { (v, c) => c.copy(isControl = true) }
    }
    p.parse(args, Config()).fold(sys.exit(1)) { config =>
      // new Convolve(config)
      if (config.isControl) new Control(config)
    }
  }
}
