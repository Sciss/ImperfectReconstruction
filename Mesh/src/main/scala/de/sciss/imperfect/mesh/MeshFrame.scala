package de.sciss.imperfect.mesh

import de.sciss.lucre.stm.Disposable
import de.sciss.lucre.synth.Sys
import de.sciss.imperfect.mesh.impl.{MeshFrameImpl => Impl}

import scala.swing.Frame

object MeshFrame {
  def apply[S <: Sys[S]](view: MeshView[S], undecorated: Boolean = false)(implicit tx: S#Tx): MeshFrame[S] =
    Impl(view, undecorated = undecorated)
}
trait MeshFrame[S <: Sys[S]] extends Disposable[S#Tx] {
  def view: MeshView[S]
  // def frame: Frame
}
