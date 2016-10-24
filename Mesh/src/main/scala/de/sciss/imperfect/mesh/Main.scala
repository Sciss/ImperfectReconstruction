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

package de.sciss.imperfect.mesh

import java.awt.event.{KeyAdapter, KeyEvent}
import java.awt.{Color, Font, Frame, Graphics, GraphicsDevice, GraphicsEnvironment, Window}
import java.io.PrintStream

object Main {
  final case class Config(verbose: Boolean = false, screenId: String = "", listScreens: Boolean = false)

  def main(args: Array[String]): Unit = {
    val p = new scopt.OptionParser[Config]("Imperfect-RaspiPlayer") {
//      opt[File]("test-video")
//        .text ("Test video file")
//        .required()
//        .action { (f, c) => c.copy(testVideo = f) }

      //      opt[Int] ('s', "start-frame")
      //        .text ("Start frame index")
      //        .action   { (v, c) => c.copy(startFrame = v) }
      //        .validate {  v     => if (v >= 0) success else failure("start-frame must be >= 0") }

      opt[String] ('s', "screen")
        .text ("Screen identifier")
        .required()
        .action   { (v, c) => c.copy(screenId = v) }

      opt[Unit] ('l', "list-screens")
        .text ("List available screens")
        .action   { (v, c) => c.copy(listScreens = true) }

      opt[Unit] ('v', "verbose")
        .action   { (v, c) => c.copy(verbose = true) }
    }
    p.parse(args, Config()).fold(sys.exit(1)) { config =>
      if (config.listScreens) {
        printScreens(Console.out)
        sys.exit()
      }
      run(config)
    }
  }

  private[this] def printScreens(out: PrintStream): Unit = {
    val screens = GraphicsEnvironment.getLocalGraphicsEnvironment.getScreenDevices
    val s = screens.map(_.getIDstring).sorted.mkString("  ", "\n  ", "")
    out.println(s)
  }

  def run(config: Config): Unit = {
    import config._
    val screens = GraphicsEnvironment.getLocalGraphicsEnvironment.getScreenDevices
    val opt1: Option[GraphicsDevice] = if (screenId.isEmpty) None else {
      val res = screens.find(_.getIDstring == screenId)
      if (res.isEmpty) {
        warn(s"screen '$screenId' not found.")
        printScreens(Console.err)
      }
      res
    }
    val screen = opt1.getOrElse {
      val opt2 = screens.find { dev =>
        val m = dev.getDisplayMode
        m.getWidth == NominalWidth && m.getHeight == NominalHeight
      }
      if (opt2.isEmpty) {
        warn(s"no screen of size $NominalWidth x $NominalHeight found")
      }
      opt2.getOrElse(screens.head)
    }

    val w = new Window(null, screen.getDefaultConfiguration) {
      override def paint(g: Graphics): Unit = {
        super.paint(g)
        paintWindow(g)
      }
    }
    w.addKeyListener(new KeyAdapter {
      override def keyTyped  (e: KeyEvent): Unit = ()
      override def keyPressed(e: KeyEvent): Unit = if (e.getKeyCode == KeyEvent.VK_ESCAPE) quit()
    })
    w.setSize(NominalWidth, NominalHeight)
    screen.setFullScreenWindow(w)
  }

  private[this] val fntTest = new Font(Font.SANS_SERIF, Font.BOLD, 500)

  def paintWindow(g: Graphics): Unit = {
    paintOffScreen()
    g.drawImage(OffScreenImg, 0, 0, NominalWidth, VisibleHeight, 0, 0, NominalWidth, VisibleHeight, null)
    g.drawImage(OffScreenImg, 0, VisibleHeight, NominalWidth, NominalHeight, NominalWidth, 0, VisibleWidth, VisibleHeight, null)
  }

  def paintOffScreen(): Unit = {
    val g2 = OffScreenG
    g2.setColor(Color.black)
    g2.fillRect(0, 0, VisibleWidth, VisibleHeight)
    g2.setColor(Color.white)
    g2.setFont(fntTest)
    val fm = g2.getFontMetrics
    g2.drawString("Imperfect Reconstruction", 20, fm.getAscent + 20)
  }

  def quit(): Unit = {
    sys.exit()
  }
}
