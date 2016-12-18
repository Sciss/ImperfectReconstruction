/*
 *  RelayTimer.scala
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

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, Locale, Timer, TimerTask}

import de.sciss.osc
import de.sciss.osc.UDP

import scala.util.control.NonFatal

object RelayTimer {
  final case class Config(selfIP        : String  = "192.168.0.66", selfPort : Int = 57111,
                          relayIP       : String  = "192.168.0.30", relayPort: Int = 57110,
                          dumpOSC       : Boolean = false,
                          simulate      : Boolean = false,
                          startHour     : Int     = 16,
                          startMinute   : Int     =  0,
                          stopHour      : Int     =  2,
                          stopMinute    : Int     =  0,
                          repeatHours   : Int     = 24,
                          repeatMinutes : Int     =  0)

  def main(args: Array[String]): Unit = {
    val default = Config()
    val p = new scopt.OptionParser[Config]("Imperfect-Relay") {
      opt[String] ("self-ip")
        .text (s"IP of this relay Pi (default: ${default.selfIP})")
        .action   { (v, c) => c.copy(selfIP = v) }

      opt[Int] ("self-port")
        .text (s"OSC receiver port of this relay Pi (default: ${default.selfPort})")
        .action   { (v, c) => c.copy(selfPort = v) }

      opt[String] ("inner-ip")
        .text (s"IP of the inner-space controller Pi (default: ${default.relayIP})")
        .action   { (v, c) => c.copy(relayIP = v) }

      opt[Int] ("inner-port")
        .text (s"OSC port of inner-space controller Pi (default: ${default.relayPort})")
        .action   { (v, c) => c.copy(relayPort = v) }

      opt[Int] ("start-hour")
        .text (s"Hour of the day to start the monitors (default: ${default.startHour})")
        .validate { v => if (v >= 0 && v < 24) success else failure(s"must be >= 0 and < 24") }
        .action   { (v, c) => c.copy(startHour = v) }

      opt[Int] ("start-minute")
        .text (s"Minute of the hour to start the monitors (default: ${default.startMinute})")
        .validate { v => if (v >= 0 && v < 60) success else failure(s"must be >= 0 and < 60") }
        .action   { (v, c) => c.copy(startMinute = v) }

      opt[Int] ("stop-hour")
        .text (s"Hour of the day to stop the monitors (default: ${default.stopHour})")
        .validate { v => if (v >= 0 && v < 24) success else failure(s"must be >= 0 and < 24") }
        .action   { (v, c) => c.copy(stopHour = v) }

      opt[Int] ("stop-minute")
        .text (s"Minute of the hour to stop the monitors (default: ${default.stopMinute})")
        .validate { v => if (v >= 0 && v < 60) success else failure(s"must be >= 0 and < 60") }
        .action   { (v, c) => c.copy(stopMinute = v) }

      opt[Int] ("repeat-hours")
        .text (s"Number of hours between repeats (default: ${default.repeatHours})")
        .validate { v => if (v >= 0) success else failure(s"must be >= 0") }
        .action   { (v, c) => c.copy(repeatHours = v) }

      opt[Int] ("repeat-minutes")
        .text (s"Number of minutes between repeats (default: ${default.repeatMinutes})")
        .validate { v => if (v >= 0) success else failure(s"must be >= 0") }
        .action   { (v, c) => c.copy(repeatMinutes = v) }

      opt[Unit] ("dump-osc")
        .text ("Enable OSC dumping")
        .action   { (_, c) => c.copy(dumpOSC = true) }

      opt[Unit] ("simulate")
        .text ("Simulate timer")
        .action   { (_, c) => c.copy(simulate = true) }
    }
    p.parse(args, Config()).fold(sys.exit(1)) { config =>
      require(config.repeatHours > 0 || config.repeatMinutes > 0, s"Either repeat-hours or repeat-minutes must be > 0")
      run(config)
    }
  }

  def run(config: Config): Unit = {
    import config._
    import osc.Implicits._
    val tConf   = UDP.Config()
    tConf.localSocketAddress  = selfIP  -> selfPort
    val target                = relayIP -> relayPort

    lazy val trns = UDP.Transmitter(tConf)

    if (dumpOSC && !simulate) {
      trns.dump()
    }

//    def shutdownSelf(): Unit = {
//      import sys.process._
//      println("Shutting down myself...")
//      Seq("sudo", "shutdown", "now").!
//    }

    val tim = new Timer

    val logHeader = new SimpleDateFormat("[d MMM yyyy, HH:mm''ss.SSS] 'RelayTimer' - ", Locale.US)

    def log(what: String): Unit = {
      println(logHeader.format(new Date()) + what)
    }

    def trySend(cmd: String): Unit = {
      val m = osc.Message("/relay", cmd)
      if (simulate) println(m) else try {
        trns.send(m, target)
      } catch {
        case NonFatal(ex) =>
          println("While sending inner-shutdown:")
          ex.printStackTrace()
      }
    }

    object Stop extends TimerTask {
      def run(): Unit = {
        log("Stopping monitors.")
        trySend("inner-shutdown")
      }
    }

    object Start extends TimerTask {
      def run(): Unit = {
        log("Turning on power...")
        trySend("inner-boot")
      }
    }

    if (!simulate) {
      trns.connect()
    }

    val now     = Calendar.getInstance()
    val startC  = now.clone().asInstanceOf[Calendar]
    val stopC   = now.clone().asInstanceOf[Calendar]

    def advance(in: Calendar): Unit =
      while (in.compareTo(now) < 0) {
        in.add(Calendar.MINUTE, config.repeatMinutes)
        in.add(Calendar.HOUR  , config.repeatHours)
      }

    startC.set(Calendar.HOUR_OF_DAY, config.startHour)
    startC.set(Calendar.MINUTE     , config.startMinute)
    startC.set(Calendar.SECOND     , 0)
    startC.set(Calendar.MILLISECOND, 0)
    advance(startC)

    stopC .set(Calendar.HOUR_OF_DAY, config.stopHour)
    stopC .set(Calendar.MINUTE     , config.stopMinute)
    stopC .set(Calendar.SECOND     , 0)
    stopC .set(Calendar.MILLISECOND, 0)
    advance(stopC)

    val startDate : Date = startC.getTime
    val stopDate  : Date = stopC .getTime
    val period    : Long = (config.repeatHours.toLong * 60 + config.repeatMinutes.toLong) * 60 * 1000

    tim.schedule(Start, startDate, period)
    tim.schedule(Stop , stopDate , period)

    println( "Timer ready.")
    println(s"- next start at $startDate")
    println(s"- next stop  at $stopDate")
  }
}