package de.sciss.imperfect.mesh
package impl

import java.awt.{Container, GraphicsEnvironment}
import java.awt.event.{ActionEvent, ActionListener}
import java.awt.image.BufferedImage
import javax.swing.{JRootPane, Timer}

import de.sciss.lucre.swing.deferTx
import de.sciss.lucre.synth.Sys
import de.sciss.swingplus.CloseOperation
import de.sciss.swingplus.Implicits._

import scala.swing.{Frame, Graphics2D}
import scala.swing.Swing._

object MeshFrameImpl {
  def apply[S <: Sys[S]](view: MeshView[S], undecorated: Boolean)
                        (implicit tx: S#Tx): MeshFrame[S] = {
    val transport = view.panel.transport
    transport.play()
    new Impl(view, undecorated = undecorated).init()
  }

  private final class Impl[S <: Sys[S]](val view: MeshView[S], undecorated: Boolean)
    extends MeshFrame[S] { impl =>

    private var _frame: Frame = _
    def frame: Frame = {
      if (_frame == null) sys.error("Frame was not yet initialized")
      _frame
    }

    def frame_=(value: Frame): Unit = {
      if (_frame != null) sys.error("Frame was already initialized")
      _frame = value
    }

    def init()(implicit tx: S#Tx): this.type = {
      deferTx(guiInit())
      this
    }

    private def guiInit(): Unit = {
      frame = new Frame {
        title = "Wolkenpumpe"
        peer.setUndecorated(impl.undecorated)
        peer.setIgnoreRepaint(true)

        contents = view.component
        this.defaultCloseOperation = CloseOperation.Exit
//        size = (800, 600)
//        centerOnScreen()
//        open()
      }

      val w = frame.peer
      val screen= GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice
      val screenConf = screen.getDefaultConfiguration
      w.setSize(screenConf.getBounds.getSize)
      screen.setFullScreenWindow(w)
//frame.open()
      w.requestFocus()

//      val panel = view.panel
//      if (panel.config.fullScreenKey) view.installFullScreenKey(frame)

      Thread.sleep(50)
      w.createBufferStrategy(2)
      Thread.sleep(50)

      val tt = new Timer(20000, new ActionListener {
        def actionPerformed(e: ActionEvent): Unit = {
//          val rootPane = w.getComponent(0).asInstanceOf[JRootPane]
//          val contentPane = rootPane.getComponent(0).asInstanceOf[Container]
//          val display = contentPane.getComponent(0)
//          println(display)
          w.remove(0)
          val display = view.panel.display
          display.reset()
          display.setSize(VisibleWidth, VisibleHeight)
        }
      })
      tt.setRepeats(false)
      tt.start()

      val strategy = w.getBufferStrategy

      var haveWarnedWinSize = false

//      var offScreenImg: BufferedImage = null
//      var offScreenG  : Graphics2D    = null

      def paintOffScreen(): Unit = {
        // val c = view.component.peer
//        if (offScreenImg == null || c.getWidth != offScreenImg.getWidth || c.getHeight != offScreenImg.getHeight) {
//          if (offScreenG   != null) offScreenG.dispose()
//          if (offScreenImg != null) offScreenImg.flush()
//          offScreenImg = new BufferedImage(c.getWidth, c.getHeight, BufferedImage.TYPE_INT_ARGB)
//          offScreenG   = offScreenImg.createGraphics()
//        }
        // c.paint(OffScreenG)
        val display = view.panel.display
        display.paintComponent(OffScreenG)
        // .paint(g.asInstanceOf[Graphics2D])
      }

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
        def actionPerformed(e: ActionEvent): Unit = {
          // if (animate) {
            //   frameIdx = frameIdx + 1
            draw()
            // }
          }
      })
      t.setRepeats(true)
      t.start()
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      view.dispose()
      deferTx(_frame.dispose())
    }
  }
}
