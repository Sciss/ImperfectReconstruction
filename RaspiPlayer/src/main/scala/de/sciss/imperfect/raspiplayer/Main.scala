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

package de.sciss.imperfect.raspiplayer

import de.sciss.file.File

import scala.util.control.NonFatal

object Main {
  private def buildInfString(key: String): String = try {
    val clazz = Class.forName("de.sciss.imperfect.raspiplayer.BuildInfo")
    val m     = clazz.getMethod(key)
    m.invoke(null).toString
  } catch {
    case NonFatal(_) => "?"
  }

  def main(args: Array[String]): Unit = {
    println(s"-- Inner Space v${buildInfString("version")}, built ${buildInfString("builtAtString")} --")

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
        .action   { (_, c) => c.copy(dumpOSC = true) }

      opt[Unit] ('c', "control")
        .text (s"Instance is control center (default ${default.isControl})")
        .action   { (_, c) => c.copy(isControl = true) }

      opt[Int] ("client-port")
        .text (s"Client OSC port (default ${default.clientPort})")
        .action   { (v, c) => c.copy(clientPort = v) }

      opt[Int] ('n', "num-clients")
        .text (s"Number of clients connected (default ${default.numClients})")
        .action { (v, c) =>
          val ids = if (c.clientIds.size == v) c.clientIds else Nil
          c.copy(numClients = v, clientIds = ids)
        }

      opt[Seq[Int]] ("clients")
        .text (s"Client identifiers (last part of 192.168.0.x)")
        .action { (v, c) =>
          c.copy(clientIds = v.toList, numClients = v.size)
        }

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

      opt[Unit] ("esc")
        .text ("Configure for esc media art lab (with horizontal screens)")
        .action   { (_, c) => c.copy(isESC = true) }

      opt[Int] ("win-x")
        .text (s"Video player window horizontal coordinate (default ${default.winX})")
        .action   { (v, c) => c.copy(winX = v) }

      opt[Int] ("win-y")
        .text (s"Video player window vertical coordinate (default ${default.winY})")
        .action   { (v, c) => c.copy(winY = v) }

//      opt[Unit] ("key-test")
//        .text ("Test key matrix")
//        .action   { (_, c) => c.copy(keyTest = true) }
//
      opt[Int] ("key-shutdown")
        .text (s"Keypad key to trigger shutdown (1 to 9; default ${default.keyShutdown})")
        .validate(i => if (i >= 1 && i <= 9) Right(()) else Left("Must be 1 to 9") )
        .action { (v, c) => c.copy(keyShutdown = (v + '0').toChar) }

      opt[Int] ("key-reboot")
        .text (s"Keypad key to trigger reboot (1 to 9; default ${default.keyReboot})")
        .validate(i => if (i >= 1 && i <= 9) Right(()) else Left("Must be 1 to 9") )
        .action { (v, c) => c.copy(keyReboot = (v + '0').toChar) }

      opt[Int] ("button-shutdown")
        .text (s"Button 8-bit integer to trigger shutdown (default ${default.keyShutdown})")
        .validate(i => if (i >= 1 && i <= 255) Right(()) else Left("Must be 1 to 255") )
        .action { (v, c) => c.copy(buttonShutdown = v) }

      opt[Int] ("button-reboot")
        .text (s"Button 8-bit integer to trigger reboot (default ${default.keyShutdown})")
        .validate(i => if (i >= 1 && i <= 255) Right(()) else Left("Must be 1 to 255") )
        .action { (v, c) => c.copy(buttonReboot = v) }
    }
    p.parse(args, default).fold(sys.exit(1)) { config =>
//      if (config.keyTest) {
//        KeyMatrix.test()
//        sys.exit()
//      }

      if (config.disableEnergySaving) {
        import sys.process._
        Seq("xset", "s", "off").!
        Seq("xset", "-dpms").!
      }

      val controlOpt = if (!config.isControl) None else {
        log("Creating control")
        implicit val screens: Screens = if (config.isESC) Screens.esc else Screens.xCoAx
        val ctl = new Control(config)
        ctl.start()

//        if (hasKeys) KeyMatrix.run() {
//          case config.keyShutdown => ctl.shutdown()
//          case config.keyReboot   => ctl.reboot()
//          case _ =>
//        }

        Some(ctl)
      }
      log("Creating player")
      new Player(config, controlOpt).start()

      if (config.hasKeys) {
        startKeys(keyShutdown = config.keyShutdown, keyReboot = config.keyReboot)
      } else if (config.hasButtons) {
        startButtons(buttonShutdown = config.buttonShutdown, buttonReboot = config.buttonReboot)
      }

      log("Ready.")
    }
  }

  def startKeys(keyShutdown: Char, keyReboot: Char): Unit = {
    import sys.process._
    val cmd = Seq("sudo", "imperfect-raspikeys", "--key-shutdown", keyShutdown.toString, "--key-reboot", keyReboot.toString)
    cmd.run()
  }

  def startButtons(buttonShutdown: Int, buttonReboot: Int): Unit = {
    import sys.process._
    val cmd = Seq("sudo", "imperfect-raspikeys", "--button-shutdown", buttonShutdown.toString, "--button-reboot", buttonReboot.toString)
    cmd.run()
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
