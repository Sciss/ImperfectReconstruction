package de.sciss.imperfect.mesh

import java.awt.event.{ActionEvent, ActionListener, KeyAdapter, KeyEvent}
import java.awt.image.BufferedImage
import java.awt.{Color, EventQueue, Font, Frame, GraphicsEnvironment}
import javax.swing.Timer

object StackOverflow extends Runnable {
  def main(args: Array[String]): Unit =
    EventQueue.invokeLater(this)

  private[this] final val fntTest       = new Font(Font.SANS_SERIF, Font.BOLD, 500)
  private[this]       var frameIdx      = 0

  private[this] final val NominalWidth  = 1920
  private[this] final val NominalHeight = 1080
  private[this] final val VisibleWidth  = 3840
  private[this] final val VisibleHeight =  540
  private[this] final val OffScreenImg  = new BufferedImage(VisibleWidth, VisibleHeight, BufferedImage.TYPE_BYTE_GRAY)
  private[this] final val OffScreenG    = OffScreenImg.createGraphics()

  def run(): Unit = {
    val screen      = GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice
    val screenConf  = screen.getDefaultConfiguration
    val w = new Frame(null, screenConf) {
      setUndecorated  (true)
      setIgnoreRepaint(true)
    }
    w.addKeyListener(new KeyAdapter {
      override def keyPressed(e: KeyEvent): Unit =
        e.getKeyCode match {
          case KeyEvent.VK_ESCAPE => sys.exit()
          case _ =>
        }
    })
    screen.setFullScreenWindow(w)
    w.createBufferStrategy(2)

    val strategy  = w.getBufferStrategy

    def draw(): Unit = {
      paintOffScreen()
      val width  = w.getWidth
      val height = w.getHeight
      do {
        do {
          val g = strategy.getDrawGraphics
          g.drawImage(OffScreenImg,            0,        0, width,        height/2,
                                               0,        0, NominalWidth, VisibleHeight, null)
          g.drawImage(OffScreenImg,            0, height/2, width,        height,
                                    NominalWidth,        0, VisibleWidth, VisibleHeight, null)
          g.dispose()
        } while (strategy.contentsRestored())
        strategy.show()
      } while (strategy.contentsLost())
    }

    val t = new Timer(12, new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        frameIdx = frameIdx + 1
        draw()
      }
    })
    t.setRepeats(true)
    t.start()
  }

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
  }
}