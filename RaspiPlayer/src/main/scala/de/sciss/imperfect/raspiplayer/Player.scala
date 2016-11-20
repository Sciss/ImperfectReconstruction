/*
 *  Player.scala
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

import java.awt.{EventQueue, Frame, GraphicsEnvironment, Point}
import java.awt.image.BufferedImage
import java.net.InetSocketAddress

import de.sciss.file._
import de.sciss.osc
import de.sciss.osc.{Packet, UDP}

import scala.util.Try
import scala.util.control.NonFatal

final class Player(config: Config) {
  @volatile
  private[this] var status: Status = Idle

  @volatile
  private[this] var play: Play = _

  private[this] val playSync = new AnyRef

  private[this] val client = {
    val c   = UDP.Config()
    c.localSocketAddress = new InetSocketAddress(config.thisHost /* InetAddress.getLocalHost() */, config.clientPort)
    val res = UDP.Client(new InetSocketAddress("192.168.0.11", 57110), c)
    res.action = received
    res
  }

  private[this] val script = config.baseDir/"dbuscontrol.sh"
  require(script.canExecute, "dbuscontrol.sh script is not executable!")

  private def sendStatus(): Unit = client ! osc.Message("/status", status.id)

  private[this] def received(p: Packet): Unit = p match {
    case PlayMessage(_play) =>
      play = _play
      status = Playing
      playSync.synchronized(playSync.notify())

    case osc.Message("/status") =>
      sendStatus()

//    case osc.Message("/test") =>
//      import sys.process._
//      Seq("omxplayer", "--loop", config.testVideo.path).run()

    case osc.Message("/shell", cmd @ _*) =>
      val cmdS = cmd.map(_.toString)
      println("Executing shell command:")
      println(cmdS.mkString(" "))
      import sys.process._
      val result = Try(cmdS.!!).toOption.getOrElse("ERROR")
      client ! osc.Message("/shell_reply", result)

    case _ =>
      Console.err.println(s"Unknown OSC message $p from control")
  }

  private def secsToHHMMSS(secs: Float): String = {
    val sec  = secs.toInt
    val secM = sec % 60
    val min  = sec / 60
    val minM = min % 60
    val hour = min / 60
    f"$hour%02d:$minM%02d:$secM%02d"
  }

  private[this] lazy val conFadeThread: Thread = new Thread {
    override def run(): Unit = {
      // ---- first stage: connect ----

      while (!client.isConnected) try {
        log("Trying to connect to server...")
        client.connect()
        log("Client connected.")
        sendStatus()
      } catch {
        case NonFatal(_) =>
          Thread.sleep(1000)
      }

      // ---- second stage: play videos ----
      while (true) {
        playSync.synchronized {
          playSync.wait()
        }

        val pl = play
        // Play(file: String, start: Float, duration: Float, orientation: Int, fadeIn: Float, fadeOut: Float)
        val cmdB = List.newBuilder[String]
        cmdB += "omxplayer"
        cmdB += "--no-osd"
        if (pl.orientation != 0) {
          cmdB += "--orientation"
          cmdB += pl.orientation.toString
        }
        if (pl.start > 0) {
          cmdB += "--pos"
          cmdB += secsToHHMMSS(pl.start)
        }
        if (pl.fadeIn > 0) {
          cmdB += "--alpha"
          cmdB += "0"
        }
        if (config.small) {
          cmdB += "--win"
          cmdB += "384,256,896,768"
        }
        cmdB += (config.baseDir/"videos"/pl.file).path

        val cmd = cmdB.result()
        import sys.process._
        val omx = cmd.run()
        val t1  = System.currentTimeMillis()

        if (pl.fadeIn > 0) {
          fadeIn(pl.fadeIn)
        }
        val t2      = System.currentTimeMillis()
        val secFdIn = (t2 - t1) / 1000.0
        val secRem  = pl.duration - secFdIn - pl.fadeOut
        val milRem  = (secRem * 1000).toLong
        Thread.sleep(milRem)
        if (pl.fadeOut > 0) {
          fadeOut(pl.fadeOut)
        }
        stopVideo()
        omx.exitValue() // wait for process to terminate

        // signalise to control
        status = Idle
        sendStatus()
      }
    }
  }

  def start(): Unit = {
    conFadeThread.start()
    if (!config.small) EventQueue.invokeLater(new Runnable {
      def run(): Unit = openBlackWindow()
    })
  }

  private def openBlackWindow(): Unit = {
    val screen      = GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice
    val screenConf  = screen.getDefaultConfiguration
    val w = new Frame(null, screenConf) {
      setUndecorated  (true)
    }
    w.setSize(screenConf.getBounds.getSize)
    screen.setFullScreenWindow(w)
    // w.requestFocus()

    // "hide" cursor
    val cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val cursor = w.getToolkit.createCustomCursor(cursorImg, new Point(0, 0), "blank")
    w.setCursor(cursor)
  }

  private def setAlpha(i: Int): Int = {
    val alpha = math.max(0, math.min(255, i))
    import sys.process._
    Seq(script.path, "setalpha", alpha.toString).!
  }

  private def fade(from: Float, to: Float, dur: Float): Unit = {
    val start = (from * 255 + 0.5f).toInt
    val end   = (to   * 255 + 0.5f).toInt
    val durM  = (dur * 1000).toInt
    val t1    = System.currentTimeMillis
    // val t3    = t1 + durM
    //     println(s"t1 = $t1, t3 = $t3, start = $start, end - $end")
    setAlpha(start)
    var last  = start
    while (last != end) {
      val t2   = System.currentTimeMillis
      val frac = math.min(1.0, (t2.toDouble - t1) / durM)
      val curr = ((frac * (to - from) + from) * 255 + 0.5).toInt
      //        val curr = (t2.linlin(t1, t3, from, to) + 0.5).toInt
      //        println(s"t2 = $t2, curr = $curr")
      if (curr != last) {
        setAlpha(curr)
        last = curr
      } else {
        // Thread.`yield()`
        Thread.sleep(0)
      }
    }
    // println("Done.")
  }

  private def fadeIn (dur: Float): Unit = fade(from = 0f, to = 1f, dur = dur)
  private def fadeOut(dur: Float): Unit = fade(from = 1f, to = 0f, dur = dur)

  private def stopVideo(): Unit = {
    import sys.process._
    Seq(script.path, "stop").!
  }
}
