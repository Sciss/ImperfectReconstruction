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

import java.awt.event.{ActionEvent, ActionListener, KeyAdapter, KeyEvent, MouseAdapter, MouseEvent}
import java.awt.{Color, EventQueue, Font, Frame, GraphicsDevice, GraphicsEnvironment}
import java.io.PrintStream
import javax.swing.Timer

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
      EventQueue.invokeLater(new Runnable { def run(): Unit = Main.run(config) })
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

    var haveWarnedWinSize = false

    val screenConf = screen.getDefaultConfiguration
    val w = new Frame(null, screenConf) {
      setUndecorated  (true)
      setIgnoreRepaint(true)
    }
    w.addKeyListener(new KeyAdapter {
      override def keyTyped  (e: KeyEvent): Unit = ()
      override def keyPressed(e: KeyEvent): Unit = {
        e.getKeyCode match {
          case KeyEvent.VK_ESCAPE => quit()
          case KeyEvent.VK_R      => drawRect = !drawRect // ; w.repaint()
          case KeyEvent.VK_A      => animate  = !animate
          case _ =>
        }
      }
    })
    w.addMouseListener(new MouseAdapter {
      override def mousePressed(e: MouseEvent): Unit = w.requestFocus()
    })
// WARNING: setSize breaks buffer-strategy based painting ?
//    w.setSize(NominalWidth, NominalHeight)
    w.setSize(screenConf.getBounds.getSize)
    screen.setFullScreenWindow(w)
    w.requestFocus()

    // Ok, so there is some weird bug in that sometime the
    // buffer doesn't have the correct size. For now, it
    // seems, waiting with the thread a bit helps.
    Thread.sleep(50)
    w.createBufferStrategy(2)
    Thread.sleep(50)

    val strategy = w.getBufferStrategy

    def draw(): Unit = {
      paintOffScreen()
      val width  = w.getWidth
      val height = w.getHeight
      do {
        do {
          val g = strategy.getDrawGraphics
          if (width == NominalWidth && height == NominalHeight) {
            g.drawImage(OffScreenImg,            0,             0, NominalWidth, VisibleHeight,
                                                 0,             0, NominalWidth, VisibleHeight, null)
            g.drawImage(OffScreenImg,            0, VisibleHeight, NominalWidth, NominalHeight,
                                      NominalWidth,             0, VisibleWidth, VisibleHeight, null)
          } else {
            if (!haveWarnedWinSize) {
              warn(s"Full screen window has dimensions $width x $height instead of $NominalWidth x $NominalHeight")
              haveWarnedWinSize = true
            }
            g.drawImage(OffScreenImg,            0,        0, width,        height/2,
                                                 0,        0, NominalWidth, VisibleHeight, null)
            g.drawImage(OffScreenImg,            0, height/2, width,        height,
                                      NominalWidth,        0, VisibleWidth, VisibleHeight, null)
            g.dispose()
          }
        } while (strategy.contentsRestored())
        strategy.show()
      } while (strategy.contentsLost())
    }

    val t = new Timer(12, new ActionListener {
      def actionPerformed(e: ActionEvent): Unit =
        if (animate) {
          frameIdx = frameIdx + 1
          draw()
        }
    })
    t.setRepeats(true)
    t.start()
  }

  private[this] val fntTest  = new Font(Font.SANS_SERIF, Font.BOLD, 500)
  private[this] var drawRect = false
  private[this] var animate  = true
  private[this] var frameIdx = 0

  def paintOffScreen(): Unit = {
    val g2 = OffScreenG
    g2.setColor(Color.black)
    g2.fillRect(0, 0, VisibleWidth, VisibleHeight)

    val atOrig = g2.getTransform
    val rot = frameIdx * Math.PI / 180
    g2.rotate(rot, VisibleWidth/2, VisibleHeight/2)
    g2.setColor(Color.white)
    g2.setFont(fntTest)
    val fm = g2.getFontMetrics
    g2.drawString("Imperfect Reconstruction", 20, fm.getAscent + 20)
    g2.setTransform(atOrig)

    if (drawRect) {
      g2.fillRect(0, 0, VisibleWidth, 10)
      g2.fillRect(0, VisibleHeight - 10, VisibleWidth, 10)
      g2.fillRect(0, 10, 10, VisibleHeight - 20)
      g2.fillRect(VisibleWidth - 10, 10, 10, VisibleHeight - 20)

      g2.setColor(Color.gray)
      g2.fillRect(VisibleWidth/2 - 10, 10, 20, VisibleHeight - 20)
    }
  }

  def quit(): Unit = {
    sys.exit()
  }
}
