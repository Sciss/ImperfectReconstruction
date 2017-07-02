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

package de.sciss.imperfect.raspikeys

import de.sciss.osc

import scala.util.control.NonFatal

object Main {
  private def buildInfString(key: String): String = try {
    val clazz = Class.forName("de.sciss.imperfect.raspikeys.BuildInfo")
    val m     = clazz.getMethod(key)
    m.invoke(null).toString
  } catch {
    case NonFatal(_) => "?"
  }

  def main(args: Array[String]): Unit = {
    println(s"-- Inner Space - RaspiKeys v${buildInfString("version")}, built ${buildInfString("builtAtString")} --")

    val default = Config()

    val p = new scopt.OptionParser[Config]("Imperfect-RaspiKeys") {
      opt[Unit] ("key-test")
        .text ("Test key matrix")
        .action   { (_, c) => c.copy(isTest = true) }

      opt[Int] ("key-shutdown")
        .text (s"Keypad key to trigger shutdown (1 to 9; default ${default.keyShutdown})")
        .validate(i => if (i >= 1 && i <= 9) Right(()) else Left("Must be 1 to 9") )
        .action { (v, c) => c.copy(keyShutdown = (v + '0').toChar) }

      opt[Int] ("key-reboot")
        .text (s"Keypad key to trigger reboot (1 to 9; default ${default.keyReboot})")
        .validate(i => if (i >= 1 && i <= 9) Right(()) else Left("Must be 1 to 9") )
        .action { (v, c) => c.copy(keyReboot = (v + '0').toChar) }
    }
    p.parse(args, default).fold(sys.exit(1)) { config =>
      if (config.isTest) {
        KeyMatrix.test()
        sys.exit()
      }

      KeyMatrix.run() {
        case config.keyShutdown => shutdown()
        case config.keyReboot   => reboot()
        case _ =>
      }

      println("keys running.")
    }
  }

  def shutdown(): Unit = sendToControl("/shutdown")
  def reboot  (): Unit = sendToControl("/reboot"  )

  private def sendToControl(cmd: String): Unit = {
    try {
      import osc.Implicits._
      val t = osc.UDP.Transmitter("192.168.0.11" -> 57110)
      t.dump()
      try {
        t.connect()
        t ! osc.Message("/forward", cmd)
      } finally {
        t.close()
      }
    } catch {
      case NonFatal(ex) =>
        ex.printStackTrace()
    }
  }
}
