package de.sciss.imperfect.mesh
package impl

import de.sciss.lucre.swing.deferTx
import de.sciss.lucre.synth.Sys
import de.sciss.swingplus.CloseOperation
import de.sciss.swingplus.Implicits._

import scala.swing.Frame
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

        contents = view.component
        this.defaultCloseOperation = CloseOperation.Exit
        size = (800, 600)
        centerOnScreen()
        open()
      }

      val panel = view.panel
      if (panel.config.fullScreenKey) view.installFullScreenKey(frame)
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      view.dispose()
      deferTx(_frame.dispose())
    }
  }
}
