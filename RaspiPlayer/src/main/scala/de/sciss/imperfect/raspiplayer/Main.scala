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
    val myIP    = Config.checkIP()
    val default = Config(thisHost = myIP, isControl = myIP == Config.controlIP)

    val p = new scopt.OptionParser[Config]("Imperfect-RaspiPlayer") {
      opt[File]("base-dir")
        .text (s"Base directory (default: ${default.baseDir})")
//        .required()
        .action { (f, c) => c.copy(baseDir = f) }

      opt[String] ('h', "host")
        .text (s"This host's IP address (default: ${default.thisHost})")
//        .required()
        .action   { (v, c) => c.copy(thisHost = v) }

      opt[Unit] ('d', "dump-osc")
        .text (s"Enable OSC dump (default ${default.dumpOSC})")
        .action   { (v, c) => c.copy(dumpOSC = true) }

      opt[Unit] ('c', "control")
        .text (s"Instance is control center (default ${default.isControl})")
        .action   { (v, c) => c.copy(isControl = true) }

      opt[Int] ("client-port")
        .text (s"Client OSC port (default ${default.clientPort})")
        .action   { (v, c) => c.copy(clientPort = v) }

      opt[Int] ('n', "num-clients")
        .text (s"Number of clients connected (default ${default.numClients})")
        .action   { (v, c) => c.copy(numClients = v) }

      opt[Unit] ("small")
        .text (s"Small display for debugging (default ${default.small})")
        .action   { (v, c) => c.copy(small = true) }
    }
    p.parse(args, default).fold(sys.exit(1)) { config =>
      // new Convolve(config)
      if (config.isControl) {
        log("Creating control")
        new Control(config).start()
      }
      log("Creating player")
      new Player(config).start()
      log("Ready.")
    }
  }
}
