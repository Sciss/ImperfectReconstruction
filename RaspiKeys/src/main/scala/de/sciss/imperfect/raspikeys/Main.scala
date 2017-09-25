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
        .text ("Test key matrix or buttons")
        .action   { (_, c) => c.copy(isTest = true) }

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
      if (config.isTest) {
        if (config.hasButtons)
          Buttons.test()
        else
          KeyMatrix.test()

        sys.exit()
      }

      if (config.hasButtons) {
        var code    = 0
        var numBits = 0
        var lastHit = System.currentTimeMillis()

        Buttons.run() { ch =>
          if (ch == '1' || ch == '2') {
            val now = System.currentTimeMillis()
            if (now - lastHit > 4000) {
              numBits = 0
            }
            lastHit = now

            if (numBits < 8) numBits += 1
            val bit = if (ch == '2') 1 else 0
            code = ((code << 1) & 0xFF) | bit

            if (numBits == 8) {
              println(s"Button code: $code")

              if      (code == config.buttonShutdown ) shutdown()
              else if (code == config.buttonReboot   ) reboot()
            }
          }
        }
        println("buttons running.")

      } else {
        KeyMatrix.run() {
          case config.keyShutdown => shutdown()
          case config.keyReboot   => reboot()
          case _ =>
        }
        println("keys running.")
      }
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
