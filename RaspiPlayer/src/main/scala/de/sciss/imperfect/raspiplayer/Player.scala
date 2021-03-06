/*
 *  Player.scala
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

import java.awt.event.{KeyAdapter, KeyEvent}
import java.awt.image.BufferedImage
import java.awt.{Color, EventQueue, Font, Frame, Graphics, GraphicsEnvironment, Point}
import java.net.InetSocketAddress

import de.sciss.file._
import de.sciss.osc
import de.sciss.osc.{Packet, UDP}

import scala.sys.process.ProcessLogger
import scala.util.Try
import scala.util.control.NonFatal

final class Player(config: Config, control: Option[Control]) {
  @volatile
  private[this] var status: Status = Idle

  @volatile
  private[this] var playIssued: Play = _

  @volatile
  private[this] var playActive: Play = _

  private[this] val playSync = new AnyRef

  @volatile
  private[this] var winX: Int = config.winX

  @volatile
  private[this] var winY: Int = config.winY

  private def mkClient(): UDP.Client = {
    val c   = UDP.Config()
    c.localSocketAddress = new InetSocketAddress(config.thisHost /* InetAddress.getLocalHost() */, config.clientPort)
    val res = UDP.Client(new InetSocketAddress("192.168.0.11", 57110), c)
    res.action = received
    res
  }

  @volatile
  private[this] var client: UDP.Client = _

  private[this] val script = config.baseDir/"dbuscontrol.sh"
//  private[this] val script = config.baseDir/"imperfect_dbus.sh"
  require(script.canExecute, s"${script.name}  - script is not executable!")

  private def sendStatus(): Unit = client ! osc.Message("/status", status.id)

  private[this] def received(p: Packet): Unit = p match {
    case PlayMessage(_play) =>
      playSync.synchronized {
        playIssued = _play
        status = Playing
        playSync.notify()
      }

    case osc.Message("/status") =>
      sendStatus()

    case osc.Message("/shell", cmd @ _*) =>
      val cmdS = cmd.map(_.toString)
      println("Executing shell command:")
      println(cmdS.mkString(" "))
      import sys.process._
      val result = Try(cmdS.!!).toOption.getOrElse("ERROR")
      client ! osc.Message("/shell_reply", result)

    case osc.Message("/color", i: Int) =>
      if (window.isDefined) EventQueue.invokeLater(new Runnable {
        def run(): Unit = {
          window.foreach(_.setBackground(new Color(i)))
        }
      })

    case osc.Message("/shutdown") =>
      log("shutting down...")
      Main.shutdown()

    case osc.Message("/reboot") =>
      log("rebooting...")
      Main.reboot()

    case osc.Message("/window", x: Int, y: Int) =>
      winX = x
      winY = y

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

  private[this] val logNone     = ProcessLogger((_: String) => ())
  private[this] val logErrors   = ProcessLogger((_: String) => (), (s: String) => Console.err.println(s))
  private[this] val hasDbusName = !config.dbusName.isEmpty
  private[this] val dbusIsInc   = config.dbusName.contains("%d")
  private[this] var dbusCount   = 0

  private def mkDbusName(inc: Boolean = false): String = {
    if (dbusIsInc) {
      if (inc) dbusCount += 1
      config.dbusName.format(dbusCount)
    } else if (hasDbusName) {
      config.dbusName
    } else {
      "org.mpris.MediaPlayer2.omxplayer"
    }
  }

  private[this] lazy val conFadeThread: Thread = new Thread {
    override def run(): Unit = {
      // ---- first stage: connect ----
      Thread.sleep(10000)

      while (client == null || !client.isConnected || !client.isOpen()) try {
        log("Trying to connect to server...")
        if (client == null || !client.isOpen()) client = mkClient()
        client.connect()
        sendStatus()
        Thread.sleep(4000)
      } catch {
        case NonFatal(_) =>
          if (client != null) {
            try {
              client.close()
            } catch {
              case NonFatal(_) =>
            }
            client = null
          }
          Thread.sleep(4000)
      }
      log("Client connected.")
      if (config.dumpOSC) {
        client.dump()
      }

      // ---- second stage: play videos ----
      while (true) {
        val pl = playSync.synchronized {
          if (playIssued == null) {
            playSync.wait()
          }
          val res = playIssued
          playActive  = res
          playIssued  = null
          res
        }

        log(s"player - $pl")
        if (pl.delay > 0) {
          Thread.sleep((pl.delay * 1000).toLong)
        }

        val videoF = config.baseDir/"videos"/pl.file
        if (!videoF.isFile) {
          println(s"WARNING: video file '$videoF' does not exist.")
        }

        // Play(file: String, start: Float, duration: Float, orientation: Int, fadeIn: Float, fadeOut: Float)
        val cmdB = List.newBuilder[String]
//        cmdB += "sudo"
        cmdB += "omxplayer"
        if (hasDbusName) {
          cmdB += "--dbus_name"
          cmdB += mkDbusName(inc = true)
        }
        cmdB += "--no-osd"
//        cmdB += "-b"
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
        } else if (winX >= 0 && winY >= 0) {
          cmdB += "--win"
          cmdB += s"$winX,$winY,${winX + 1024},${winY + 1024}"
        }
        cmdB += videoF.path

        val cmd = cmdB.result()
        log(cmd.mkString(" "))
        import sys.process._
        val omx = cmd.run(logErrors)
        val t1  = System.currentTimeMillis()

        // it takes a moment till the dbus client is registered.
        // if we run the script with the 'status' command, it will
        // have an error code of 1 while the client is not yet
        // visible, and zero when it's there.
        var launched = false
        while (!launched && (System.currentTimeMillis() - t1 < 2000)) {
          val res = runScript("status" :: Nil, ignoreError = true)
          launched = res == 0
          if (!launched) Thread.sleep(0)
        }
        log(s"player - omx launched")

        if (pl.fadeIn > 0) {
          fadeIn(pl.fadeIn)
        }
        val t2      = System.currentTimeMillis()
        val secFdIn = (t2 - t1) / 1000.0
        val secRem  = pl.duration - secFdIn - pl.fadeOut
        val milRem  = math.max(0L, (secRem * 1000).toLong)
        Thread.sleep(milRem)
        if (pl.fadeOut > 0) {
          fadeOut(pl.fadeOut)
        }
        log(s"player - stopping omx")
        stopVideo()
        omx.exitValue() // wait for process to terminate
        log(s"player - omx stopped")
        Thread.sleep(1000)  // XXX TODO --- does this help with dbus registry?

        // signalise to control
        status = Idle
        sendStatus()
      }
    }
  }

  @volatile
  private[this] var window = Option.empty[Frame]

  def start(): Unit = {
    conFadeThread.start()
    if (!config.small) EventQueue.invokeLater(new Runnable {
      def run(): Unit = openBlackWindow()
    })
  }

  @volatile
  private[this] var debugMessage = ""

  private def debugThreads(): Unit = {
    import scala.collection.JavaConverters._
    val m = Thread.getAllStackTraces.asScala.toVector.sortBy(_._1.getId)
    println("Id__ State_________ Name___________________ Pri")
    m.foreach { case (t, _) =>
      println(f"${t.getId}%3d  ${t.getState}%-13s  ${t.getName}%-23s  ${t.getPriority}%2d")
    }
    m.foreach { case (t, stack) =>
      println()
      println(f"${t.getId}%3d  ${t.getState}%-13s  ${t.getName}%-23s")
      if (t == Thread.currentThread()) println("    (self)")
      else stack.foreach { elem =>
        println(s"    $elem")
      }
    }
  }

  private def openBlackWindow(): Unit = {
    val screen      = GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice
    val screenConf  = screen.getDefaultConfiguration
    val fnt         = new Font(Font.SANS_SERIF, Font.BOLD, 36)
    val w = new Frame(null, screenConf) {
      override def paint(g: Graphics): Unit = {
        super.paint(g)
        val m = debugMessage
        if (!m.isEmpty) {
          g.setFont(fnt)
          val fm = g.getFontMetrics
          val tw = fm.stringWidth(debugMessage)
          g.setColor(Color.white)
          g.drawString(m, (getWidth - tw)/2, getHeight/2 - fm.getAscent)
        }
      }
    }
    w.setUndecorated  (true)
//    w.setIgnoreRepaint(true)
    w.setBackground(new Color(config.background))
    w.addKeyListener(new KeyAdapter {
      override def keyPressed(e: KeyEvent): Unit = {
        e.getKeyCode match {
          case KeyEvent.VK_ESCAPE => control.foreach(_.shutdown())
          case KeyEvent.VK_Q      => sys.exit()  // 'tschack!
          case KeyEvent.VK_R      => control.foreach(_.reboot())
          case KeyEvent.VK_T      =>
            debugMessage = if (playActive == null) "null" else playActive.toString
            w.repaint()
            debugThreads()

//          case KeyEvent.VK_A      => animate  = !animate
//          case KeyEvent.VK_C      => controlWindow.open()

          case _ =>
        }
      }
    })
    w.setSize(screenConf.getBounds.getSize)
    screen.setFullScreenWindow(w)
    w.requestFocus()

    // "hide" cursor
    val cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val cursor = w.getToolkit.createCustomCursor(cursorImg, new Point(0, 0), "blank")
    w.setCursor(cursor)
    window = Some(w)
  }

  private def setAlpha(i: Int): Int = {
    val alpha = math.max(0, math.min(255, i))
    runScript("setalpha" :: alpha.toString :: Nil)
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

  private def runScript(args: List[String], ignoreError: Boolean = false): Int = {
    import sys.process._
//    val name = mkDbusName()
//    val pb = Process(script.path :: args, None, "OMXPLAYER_DBUS_NAME" -> name)
//    val pb = Process("sudo" :: script.path :: args, None, "OMXPLAYER_DBUS_NAME" -> name)
    val pb = script.path :: args
    val res = if (ignoreError) pb.!<(logNone) else pb.!
    res
  }

  private def stopVideo(): Unit = runScript("stop" :: Nil)
}
