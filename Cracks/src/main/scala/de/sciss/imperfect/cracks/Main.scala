/*
 *  Main.scala
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

package de.sciss.imperfect.cracks

import de.sciss.file.File

import scala.util.control.NonFatal

object Main {
  private def buildInfString(key: String): String = try {
    val clazz = Class.forName("de.sciss.imperfect.cracks.BuildInfo")
    val m     = clazz.getMethod(key)
    m.invoke(null).toString
  } catch {
    case NonFatal(_) => "?"
  }

  def main(args: Array[String]): Unit = {
    println(s"-- Imperfect Reconstruction Cracks v${buildInfString("version")}, built ${buildInfString("builtAtString")} --")

    val myIP            = Config.checkIP()
    val defaultChannel  = Config.defaultChannels.getOrElse(myIP, 0)
    val default         = Config(thisHost = myIP, isControl = myIP == Config.controlIP, thisChannel = defaultChannel)

    val p = new scopt.OptionParser[Config]("Imperfect-Cracks") {
      opt[File]("base-dir")
        .text (s"Base directory (default: ${default.baseDir})")
        .action { (f, c) => c.copy(baseDir = f) }

      opt[String] ('h', "host")
        .text (s"This host's IP address (default: ${default.thisHost})")
        .action   { (v, c) => c.copy(thisHost = v) }

      opt[Unit] ('d', "dump-osc")
        .text (s"Enable OSC dump (default ${default.dumpOSC})")
        .action   { (_, c) => c.copy(dumpOSC = true) }

      opt[Unit] ('c', "control")
        .text (s"Instance is control center (default ${default.isControl})")
        .action   { (_, c) => c.copy(isControl = true) }

      opt[Int] ("client-port")
        .text (s"Client OSC port (default ${default.clientPort})")
        .action   { (v, c) => c.copy(clientPort = v) }

      opt[Int] ('n', "num-clients")
        .text (s"Number of clients connected (default ${default.numClients})")
        .action   { (v, c) => c.copy(numClients = v) }

      opt[Unit] ("small")
        .text (s"Small display for debugging (default ${default.small})")
        .action   { (_, c) => c.copy(small = true) }

      opt[Unit] ("keep-energy")
        .text ("Do not turn off energy saving")
        .action   { (_, c) => c.copy(disableEnergySaving = false) }

      opt[Int] ("background")
        .text (s"Background color 0xRRGGBB (default ${default.background.toHexString})")
        .action   { (v, c) => c.copy(background = v) }

      opt[String] ("dbus")
        .text (s"dbus name for omxplayer (default: ${default.dbusName})")
        .action   { (v, c) => c.copy(dbusName = v) }

      opt[Int] ("channel")
        .text (s"Sound channel, counting from zero (default ${default.thisChannel})")
        .action   { (v, c) => c.copy(thisChannel = v) }

      opt[Int] ("fps")
        .text (s"Animation speed in fps (default ${default.fps})")
        .action   { (v, c) => c.copy(fps = v) }

      opt[Int] ("fade-dur")
        .text (s"Fade duration in seconds (default ${default.fadeDur})")
        .action   { (v, c) => c.copy(fadeDur = v) }

      opt[Int] ("max-trace")
        .text (s"Maximum trace length (default ${default.maxTrace})")
        .action   { (v, c) => c.copy(maxTrace = v) }

      opt[Int] ("trace-color")
        .text (s"Trace color 0xRRGGBB (default ${default.traceColor.toHexString})")
        .action   { (v, c) => c.copy(traceColor = v) }

      opt[Int] ("trace-width")
        .text (s"Trace stroke width (default ${default.traceWidth})")
        .action   { (v, c) => c.copy(traceWidth = v) }
    }
    p.parse(args, default).fold(sys.exit(1)) { config =>
      if (config.disableEnergySaving) {
        import sys.process._
        Seq("xset", "s", "off").!
        Seq("xset", "-dpms").!
      }

      val controlOpt = if (!config.isControl) None else {
        log("Creating control")
        val ctl = new Control(config)
        ctl.start()
        Some(ctl)
      }
      log(s"Creating player at /${config.thisHost}:${config.clientPort}")
      new Player(config, controlOpt).start()
      log("Ready.")
    }
  }

  def shutdown(): Unit = {
    import sys.process._
    Seq("sudo", "shutdown", "now").run()
  }

  def reboot(): Unit = {
    import sys.process._
    Seq("sudo", "reboot", "now").run()
  }
}
