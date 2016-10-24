package de.sciss.imperfect.mesh

import de.sciss.imperfect.mesh.impl.{MeshViewImpl => Impl}
import de.sciss.lucre.stm
import de.sciss.lucre.swing.View
import de.sciss.lucre.synth.Sys
import de.sciss.nuages.{ControlPanel, Nuages, NuagesPanel, ScissProcs}
import de.sciss.synth.proc.{AuralSystem, WorkspaceHandle}

import scala.swing.Component

object MeshView {
  def apply[S <: Sys[S]](nuages: Nuages[S], nuagesConfig: Nuages.Config, scissConfig: ScissProcs.Config)
                        (implicit tx: S#Tx, aural: AuralSystem, workspace: WorkspaceHandle[S],
                         cursor: stm.Cursor[S]): MeshView[S] =
    Impl(nuages, nuagesConfig, scissConfig)
}
trait MeshView[S <: Sys[S]] extends View.Cursor[S] {
  def panel: MeshPanel[S]
  def controlPanel: ControlPanel

  def installFullScreenKey(frame: scala.swing.Window): Unit

  def addSouthComponent(c: Component): Unit
}
