/*
 *  Relay.scala
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

package de.sciss.imperfect.relay

import java.io.File

import de.sciss.osc
import de.sciss.osc.UDP

import scala.util.control.NonFatal

object Relay {
  final case class Config(selfIP : String = "192.168.0.30", selfPort : Int = 57110,
                          selfSendPort: Int = 57111,
                          innerIP: String = "192.168.0.11", innerPort: Int = 57110,
                          dumpOSC: Boolean = false,
                          shutdownTime: Int = 60,
                          bootTime    : Int = 90,
                          gpioScript: File = new File("gpio.sh"),
                          gpioPin: Int = 18)

  def main(args: Array[String]): Unit = {
    val default = Config()
    val p = new scopt.OptionParser[Config]("Imperfect-Relay") {
      opt[String] ("self-ip")
        .text (s"IP of this relay Pi (default: ${default.selfIP}")
        .action   { (v, c) => c.copy(selfIP = v) }

      opt[Int] ("self-port")
        .text (s"OSC receiver port of this relay Pi (default: ${default.selfPort})")
        .action   { (v, c) => c.copy(selfPort = v) }

      opt[Int] ("self-send-port")
        .text (s"OSC transmitter port of this relay Pi (default: ${default.selfSendPort})")
        .action   { (v, c) => c.copy(selfSendPort = v) }

      opt[String] ("inner-ip")
        .text (s"IP of the inner-space controller Pi (default: ${default.innerIP}")
        .action   { (v, c) => c.copy(innerIP = v) }

      opt[Int] ("inner-port")
        .text (s"OSC port of inner-space controller Pi (default: ${default.innerPort})")
        .action   { (v, c) => c.copy(innerPort = v) }

      opt[File] ("gpio")
        .text (s"Location of gpio script (default: ${default.gpioScript}")
        .validate { v => if (v.isFile) success else failure(s"GPIO script '$v' not found!") }
        .action   { (v, c) => c.copy(gpioScript = v) }

      opt[Int] ("pin")
        .text (s"GPIO pin for relay (default: ${default.gpioPin}")
        .validate { v => if (v >= 4 && v <= 26) success else failure(s"must be >= 4 and <= 26") }
        .action   { (v, c) => c.copy(gpioPin = v) }

      opt[Unit] ("dump-osc")
        .text ("Enable OSC dumping")
        .action   { (_, c) => c.copy(dumpOSC = true) }
    }
    p.parse(args, Config()).fold(sys.exit(1)) { config =>
      run(config)
    }
  }

  def run(config: Config): Unit = {
    import config._
    import osc.Implicits._
    val rConf   = UDP.Config()
    rConf.localSocketAddress  = selfIP  -> selfPort
    val tConf   = UDP.Config()
    tConf.localSocketAddress  = selfIP  -> selfSendPort
    val target                = innerIP -> innerPort

    val r       = UDP.Receiver   (rConf)
    val t       = UDP.Transmitter(tConf)

    if (dumpOSC) {
      r.dump()
      t.dump()
    }

    def shutdownSelf(): Unit = {
      import sys.process._
      println("Shutting down myself...")
      Seq("sudo", "shutdown", "now").!
    }

    def shutdownInner(): Unit = {
      println("Shutting down inner...")
      try {
        t.send(osc.Message("/forward", "/shutdown"), target)
      } catch {
        case NonFatal(ex) =>
          println("While sending inner-shutdown:")
          ex.printStackTrace()
      }
      Thread.sleep(shutdownTime.toLong * 1000)
      println("Turning off power...")
      import sys.process._
      val res0 = Seq("sudo", gpioScript.getPath, "mode" , gpioPin.toString, "out").!
      val res1 = Seq("sudo", gpioScript.getPath, "write", gpioPin.toString, 0.toString).!
      if (res0 == 0 && res1 == 0) println("Ok.") else Console.err.println("Script failed!")
    }

    def startInner(): Unit = {
      println("Turning on power...")
      import sys.process._
      val res1 = Seq("sudo", gpioScript.getPath, "write", gpioPin.toString, 1.toString).!
      if (res1 != 0) Console.err.println("Script failed!")
      Thread.sleep(bootTime.toLong * 1000)
      println("Ready.")
    }

    def rebootInner(): Unit = {
      println("Rebooting inner...")
      try {
        t.send(osc.Message("/forward", "/reboot"), target)
      } catch {
        case NonFatal(ex) =>
          println("While sending inner-reboot:")
          ex.printStackTrace()
      }
      Thread.sleep((shutdownTime + bootTime).toLong * 1000)
      println("Ready.")
    }

    r.action = {
      case (osc.Message("/relay", "inner-reboot"  ), _) => rebootInner()
      case (osc.Message("/relay", "inner-shutdown"), _) => shutdownInner()
      case (osc.Message("/relay", "inner-boot"    ), _) => startInner()
      case (osc.Message("/relay", "shutdown"      ), _) => shutdownSelf()

      case (other, addr) =>
        Console.err.println(s"Unknown OSC packet: $other")
        try {
          t.send(osc.Message("/error", other.toString), addr)
        } catch {
          case NonFatal(ex) =>
            ex.printStackTrace()
        }
    }

    t.connect()
    r.connect()

    println("Relay ready.")
  }
}