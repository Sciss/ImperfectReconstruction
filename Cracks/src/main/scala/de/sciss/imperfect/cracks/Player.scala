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

package de.sciss.imperfect.cracks

import java.awt.event.{KeyAdapter, KeyEvent}
import java.awt.{Color, EventQueue, Frame, GraphicsEnvironment}
import java.net.{InetSocketAddress, SocketAddress}

import de.sciss.osc
import de.sciss.osc.{Packet, UDP}

import scala.util.Try

final class Player(config: Config, control: Option[Control]) {
  private def mkClient(): UDP.Receiver.Undirected /* Client */ = {
    val c   = UDP.Config()
    c.localSocketAddress = new InetSocketAddress(config.thisHost /* InetAddress.getLocalHost() */, config.clientPort)
//    val res = UDP.Client(new InetSocketAddress("192.168.0.11", 57110), c)
    val res = UDP.Receiver(c)
    res.action = received
    res
  }

  @volatile
  private[this] var client: UDP.Receiver.Undirected /* Client */ = _

//  private[this] val script = config.baseDir/"dbuscontrol.sh"
//  require(script.canExecute, s"${script.name}  - script is not executable!")

  private[this] def received(p: Packet, sender: SocketAddress): Unit = p match {
    case osc.Message("/shell", cmd @ _*) =>
      val cmdS = cmd.map(_.toString)
      println("Executing shell command:")
      println(cmdS.mkString(" "))
      import sys.process._
      /* val result = */ Try(cmdS.!!).toOption.getOrElse("ERROR")
      // client ! osc.Message("/shell_reply", result)

    case osc.Message("/color", i: Int) =>
      if (window.isDefined) EventQueue.invokeLater { () =>
        window.foreach(_.setBackground(new Color(i)))
      }

    case osc.Message("/shutdown") =>
      log("shutting down...")
      Main.shutdown()

    case osc.Message("/reboot") =>
      log("rebooting...")
      Main.reboot()

    case _ =>
      Console.err.println(s"Unknown OSC message $p from control")
  }

//  private def secsToHHMMSS(secs: Float): String = {
//    val sec  = secs.toInt
//    val secM = sec % 60
//    val min  = sec / 60
//    val minM = min % 60
//    val hour = min / 60
//    f"$hour%02d:$minM%02d:$secM%02d"
//  }

//  private[this] val logNone     = ProcessLogger((_: String) => ())
//  private[this] val logErrors   = ProcessLogger((_: String) => (), (s: String) => Console.err.println(s))
//  private[this] val hasDbusName = !config.dbusName.isEmpty
//  private[this] val dbusIsInc   = config.dbusName.contains("%d")
//  private[this] var dbusCount   = 0

//  private def mkDbusName(inc: Boolean = false): String = {
//    if (dbusIsInc) {
//      if (inc) dbusCount += 1
//      config.dbusName.format(dbusCount)
//    } else if (hasDbusName) {
//      config.dbusName
//    } else {
//      "org.mpris.MediaPlayer2.omxplayer"
//    }
//  }

  @volatile
  private[this] var window = Option.empty[Frame]

  def start(): Unit = {
    if (client == null || !client.isOpen()) client = mkClient()
    client.connect()
//    sendStatus()
    // conFadeThread.start()
    if (!config.small) EventQueue.invokeLater(() => openWindow())
  }

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

  private def openWindow(): Unit = {
    val screen      = GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice
    val screenConf  = screen.getDefaultConfiguration
//    val fnt         = new Font(Font.SANS_SERIF, Font.BOLD, 36)
    val w = new PlayerFrame(config, screen, screenConf)
    w.addKeyListener(new KeyAdapter {
      override def keyPressed(e: KeyEvent): Unit = {
        e.getKeyCode match {
          case KeyEvent.VK_ESCAPE => control.foreach(_.shutdown())
          case KeyEvent.VK_Q      => sys.exit()  // 'tschack!
          case KeyEvent.VK_R      => control.foreach(_.reboot())
          case KeyEvent.VK_T      =>
            // debugMessage = if (playActive == null) "null" else playActive.toString
            w.repaint()
            debugThreads()

//          case KeyEvent.VK_A      => animate  = !animate
//          case KeyEvent.VK_C      => controlWindow.open()

          case _ =>
        }
      }
    })
    w.fullScreen()
    window = Some(w)
  }

//  private def setAlpha(i: Int): Int = {
//    val alpha = math.max(0, math.min(255, i))
//    runScript("setalpha" :: alpha.toString :: Nil)
//  }

//  private def fade(from: Float, to: Float, dur: Float): Unit = {
//    val start = (from * 255 + 0.5f).toInt
//    val end   = (to   * 255 + 0.5f).toInt
//    val durM  = (dur * 1000).toInt
//    val t1    = System.currentTimeMillis
//    // val t3    = t1 + durM
//    //     println(s"t1 = $t1, t3 = $t3, start = $start, end - $end")
//    setAlpha(start)
//    var last  = start
//    while (last != end) {
//      val t2   = System.currentTimeMillis
//      val frac = math.min(1.0, (t2.toDouble - t1) / durM)
//      val curr = ((frac * (to - from) + from) * 255 + 0.5).toInt
//      //        val curr = (t2.linlin(t1, t3, from, to) + 0.5).toInt
//      //        println(s"t2 = $t2, curr = $curr")
//      if (curr != last) {
//        setAlpha(curr)
//        last = curr
//      } else {
//        // Thread.`yield()`
//        Thread.sleep(0)
//      }
//    }
//    // println("Done.")
//  }

//  private def fadeIn (dur: Float): Unit = fade(from = 0f, to = 1f, dur = dur)
//  private def fadeOut(dur: Float): Unit = fade(from = 1f, to = 0f, dur = dur)

//  private def runScript(args: List[String], ignoreError: Boolean = false): Int = {
//    import sys.process._
////    val name = mkDbusName()
////    val pb = Process(script.path :: args, None, "OMXPLAYER_DBUS_NAME" -> name)
////    val pb = Process("sudo" :: script.path :: args, None, "OMXPLAYER_DBUS_NAME" -> name)
//    val pb = script.path :: args
//    val res = if (ignoreError) pb.!<(logNone) else pb.!
//    res
//  }

//  private def stopVideo(): Unit = runScript("stop" :: Nil)
}
