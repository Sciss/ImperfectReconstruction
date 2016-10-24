package de.sciss.imperfect.mesh

import java.awt.geom.Point2D

import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Obj, Sys, TxnLike}
import de.sciss.lucre.swing.View
import de.sciss.lucre.synth.{AudioBus, Synth, Txn, Node => SNode, Sys => SSys}
import de.sciss.nuages.{ExponentialWarp, Nuages, NuagesAttribute, NuagesContext, NuagesNode, NuagesObj, NuagesOutput, NuagesPanel, OverlayPanel, ParamSpec}
import de.sciss.imperfect.mesh.impl.{MeshPanelImpl => Impl}
import de.sciss.synth.proc.{AuralSystem, Output, Transport, WorkspaceHandle}
import prefuse.data.Graph
import prefuse.visual.{AggregateTable, VisualGraph}
import prefuse.{Display, Visualization}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.Point

object MeshPanel {
  var verbose = false

  val GROUP_GRAPH     = "graph"
  val COL_NUAGES      = "nuages"
  val GROUP_SELECTION = "sel"

  final val masterAmpSpec       = ParamSpec(0.01, 10.0, ExponentialWarp) -> 1.0
  final val soloAmpSpec         = ParamSpec(0.10, 10.0, ExponentialWarp) -> 0.5

  def apply[S <: SSys[S]](nuages: Nuages[S], config: Nuages.Config)
                         (implicit tx: S#Tx, aural: AuralSystem,
                          cursor: stm.Cursor[S], workspace: WorkspaceHandle[S],
                          context: NuagesContext[S]): MeshPanel[S] =
    Impl(nuages, config)
}
trait MeshPanel[S <: Sys[S]] extends NuagesPanel[S] {

//  def aural: AuralSystem
//
//  def transport: Transport[S]
//
//  def cursor: stm.Cursor[S]
//
//  def config : Nuages.Config
//
//  implicit def context: NuagesContext[S]
//
//  def isTimeline: Boolean
//
//  // ---- methods to be called on the EDT ----
//
//  // -- prefuse --
//
//  def display       : Display
//  def visualization : Visualization
//  def graph         : Graph
//  def visualGraph   : VisualGraph
//  def aggrTable     : AggregateTable
//
//  // -- dialogs --
//
//  def showCreateGenDialog(pt: Point): Boolean
//  def showInsertFilterDialog(vOut: NuagesOutput[S], vIn: NuagesAttribute[S], pt: Point): Boolean
//  def showAppendFilterDialog(vOut: NuagesOutput[S], pt: Point): Boolean
//  def showInsertMacroDialog(): Boolean
//
//  def showOverlayPanel(p: OverlayPanel, pt: Option[Point] = None): Boolean
//
//  def setSolo(vp: NuagesObj[S], onOff: Boolean): Unit
//
//  def selection: Set[NuagesNode[S]]
//
//  def saveMacro(name: String, obj: Set[NuagesObj[S]]): Unit
//
//  // ---- transactional methods ----
//
//  def nuages(implicit tx: S#Tx): Nuages[S]
//
//  def setMasterVolume(v: Double)(implicit tx: S#Tx): Unit
//  def setSoloVolume  (v: Double)(implicit tx: S#Tx): Unit
//
//  def masterSynth(implicit tx: Txn): Option[Synth]
//  def masterSynth_=(value: Option[Synth])(implicit tx: Txn): Unit
//
//  def mkPeakMeter (bus: AudioBus, node: SNode)(fun: Double      => Unit)(implicit tx: S#Tx): Synth
//  def mkValueMeter(bus: AudioBus, node: SNode)(fun: Vec[Double] => Unit)(implicit tx: S#Tx): Synth
//
//  /** Schedule code to be executed during paused visualization animation
//    * on the EDT after the commit of the transaction.
//    */
//  def deferVisTx(thunk: => Unit)(implicit tx: TxnLike): Unit
//
//  def createGenerator(gen: Obj[S], colOpt: Option[Obj[S]], pt: Point2D)(implicit tx: S#Tx): Unit
//
//  def registerNode  (id: S#ID, view: NuagesObj[S])(implicit tx: S#Tx): Unit
//  def unregisterNode(id: S#ID, view: NuagesObj[S])(implicit tx: S#Tx): Unit
//
//  def appendFilter(pred: Output[S], fltSrc: Obj[S], colSrcOpt: Option[Obj[S]], fltPt: Point2D)
//                  (implicit tx: S#Tx): Unit
//
//  def insertFilter(pred: Output[S], succ: NuagesAttribute[S], fltSrc: Obj[S], fltPt: Point2D)
//                  (implicit tx: S#Tx): Unit
}