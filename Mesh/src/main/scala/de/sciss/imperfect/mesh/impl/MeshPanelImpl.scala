package de.sciss.imperfect.mesh
package impl

import java.awt.Color

import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Disposable, Obj, TxnLike}
import de.sciss.lucre.swing.{ListView, defer, deferTx, requireEDT}
import de.sciss.lucre.synth.{AudioBus, Node, Sys}
import de.sciss.nuages.Nuages.Surface
import de.sciss.nuages.impl.{PanelImplDialogs, PanelImplFolderInit, PanelImplGuiInit, PanelImplMixer, PanelImplReact, PanelImplTimelineInit, PanelImplTxnFuns}
import de.sciss.nuages.{KeyControl, Nuages, NuagesAttribute, NuagesContext, NuagesNode, NuagesObj, NuagesPanel}
import de.sciss.synth.proc
import de.sciss.synth.proc.{AuralObj, AuralSystem, Folder, Proc, Timeline, Transport, WorkspaceHandle}
import prefuse.controls.Control
import prefuse.visual.NodeItem

import scala.collection.breakOut
import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.stm.{Ref, TxnLocal}
import scala.util.control.NonFatal

object MeshPanelImpl {
  var DEBUG = false

  def apply[S <: Sys[S]](nuages: Nuages[S], config: Nuages.Config)
                        (implicit tx: S#Tx, aural: AuralSystem, cursor: stm.Cursor[S],
                         workspace: WorkspaceHandle[S], context: NuagesContext[S]): MeshPanel[S] = {
    val nuagesH       = tx.newHandle(nuages)

    val listGen       = mkListView(nuages.generators)
    val listFlt1      = mkListView(nuages.filters   )
    val listCol1      = mkListView(nuages.collectors)
    val listFlt2      = mkListView(nuages.filters   )
    val listCol2      = mkListView(nuages.collectors)
    val listMacro     = mkListView(nuages.macros    )

    val nodeMap       = tx.newInMemoryIDMap[NuagesObj[S]]
    // val scanMap       = tx.newInMemoryIDMap[NuagesOutput[S]] // ScanInfo [S]]
    val missingScans  = tx.newInMemoryIDMap[List[NuagesAttribute[S]]]
    val transport     = Transport[S](aural)
    val surface       = nuages.surface
    // transport.addObject(surface.peer)

    surface match {
      case Surface.Timeline(tl) =>
        ???
        new PanelImplTimeline[S](nuagesH, nodeMap, /* scanMap, */ missingScans, config, transport, aural,
          listGen = listGen, listFlt1 = listFlt1, listCol1 = listCol1, listFlt2 = listFlt2, listCol2 = listCol2,
          listMacro = listMacro)
          .init(tl)

      case Surface.Folder(f) =>
        new PanelImplFolder[S](nuagesH, nodeMap, /* scanMap, */ missingScans, config, transport, aural,
          listGen = listGen, listFlt1 = listFlt1, listCol1 = listCol1, listFlt2 = listFlt2, listCol2 = listCol2,
          listMacro = listMacro)
          .init(f)
    }
  }

  final val GROUP_NODES   = "graph.nodes"
  final val GROUP_EDGES   = "graph.edges"

  final val AGGR_PROC     = "aggr"

  final val ACTION_LAYOUT = "layout"
  final val ACTION_COLOR  = "color"
  final val LAYOUT_TIME   = 50

  def mkListView[S <: Sys[S]](folderOpt: Option[Folder[S]])
                             (implicit tx: S#Tx, cursor: stm.Cursor[S]): ListView[S, Obj[S], Unit] = {
    import proc.Implicits._
    val h = ListView.Handler[S, Obj[S], Unit /* Obj.Update[S] */] { implicit tx => obj => obj.name } (_ => (_, _) => None)
    implicit val ser = de.sciss.lucre.expr.List.serializer[S, Obj[S] /* , Unit */ /* Obj.Update[S] */]
    // val res = ListView[S, Obj[S], Unit /* Obj.Update[S] */, String](folder, h)
    val res = ListView.empty[S, Obj[S], Unit /* Obj.Update[S] */, String](h)
    deferTx {
      val c = res.view
      c.background = Color.black
      c.foreground = Color.white
      c.selectIndices(0)
    }
    res.list = folderOpt
    res
  }
}

final class PanelImplTimeline[S <: Sys[S]](protected val nuagesH: stm.Source[S#Tx, Nuages[S]],
                                           protected val nodeMap: stm.IdentifierMap[S#ID, S#Tx, NuagesObj[S]],
                                           //     protected val scanMap: stm.IdentifierMap[S#ID, S#Tx, NuagesOutput[S]],
                                           protected val missingScans: stm.IdentifierMap[S#ID, S#Tx, List[NuagesAttribute[S]]],
                                           val config   : Nuages.Config,
                                           val transport: Transport[S],
                                           val aural    : AuralSystem,
                                           protected val listGen  : ListView[S, Obj[S], Unit /* Obj.Update[S] */],
                                           protected val listFlt1 : ListView[S, Obj[S], Unit /* Obj.Update[S] */],
                                           protected val listCol1 : ListView[S, Obj[S], Unit /* Obj.Update[S] */],
                                           protected val listFlt2 : ListView[S, Obj[S], Unit /* Obj.Update[S] */],
                                           protected val listCol2 : ListView[S, Obj[S], Unit /* Obj.Update[S] */],
                                           protected val listMacro: ListView[S, Obj[S], Unit /* Obj.Update[S] */])
                                          (implicit val cursor: stm.Cursor[S],
                                           protected val workspace: WorkspaceHandle[S],
                                           val context: NuagesContext[S])
  extends PanelImpl[S, Timeline[S], AuralObj.Timeline[S]]
    with PanelImplTimelineInit[S] with MeshPanel[S]

final class PanelImplFolder[S <: Sys[S]](protected val nuagesH: stm.Source[S#Tx, Nuages[S]],
                                         protected val nodeMap: stm.IdentifierMap[S#ID, S#Tx, NuagesObj[S]],
                                         //     protected val scanMap: stm.IdentifierMap[S#ID, S#Tx, NuagesOutput[S]],
                                         protected val missingScans: stm.IdentifierMap[S#ID, S#Tx, List[NuagesAttribute[S]]],
                                         val config   : Nuages.Config,
                                         val transport: Transport[S],
                                         val aural    : AuralSystem,
                                         protected val listGen  : ListView[S, Obj[S], Unit /* Obj.Update[S] */],
                                         protected val listFlt1 : ListView[S, Obj[S], Unit /* Obj.Update[S] */],
                                         protected val listCol1 : ListView[S, Obj[S], Unit /* Obj.Update[S] */],
                                         protected val listFlt2 : ListView[S, Obj[S], Unit /* Obj.Update[S] */],
                                         protected val listCol2 : ListView[S, Obj[S], Unit /* Obj.Update[S] */],
                                         protected val listMacro: ListView[S, Obj[S], Unit /* Obj.Update[S] */])
                                        (implicit val cursor: stm.Cursor[S],
                                         protected val workspace: WorkspaceHandle[S],
                                         val context: NuagesContext[S])
  extends PanelImpl[S, Folder[S], AuralObj.Folder[S]]
    with PanelImplFolderInit[S] with MeshPanel[S]

// nodeMap: uses timed-id as key
trait PanelImpl[S <: Sys[S], Repr <: Obj[S], AuralRepr <: AuralObj[S]]
  extends MeshPanel[S]
    // here comes your cake!
    with PanelImplDialogs[S]
    with PanelImplTxnFuns[S]
    with PanelImplReact  [S]
    with PanelImplMixer  [S]
//    with PanelImplGuiInit[S]
    with MeshPanelImplGuiInit[S]
    // with PanelImplGuiFuns[S]
{
  panel =>

  import MeshPanel.{COL_NUAGES, GROUP_SELECTION}
  import TxnLike.peer

  // ---- abstract ----

  protected def nuagesH: stm.Source[S#Tx, Nuages[S]]

  protected def auralReprRef: Ref[Option[AuralRepr]]

  protected def disposeTransport()(implicit tx: S#Tx): Unit

  protected def initObservers(repr: Repr)(implicit tx: S#Tx): Unit

  // ---- impl ----

  protected final def main: MeshPanel[S] = this

  protected final var observers     = List.empty[Disposable[S#Tx]]
  protected final val auralObserver = Ref(Option.empty[Disposable[S#Tx]])

  //  protected final val auralToViewMap  = TMap.empty[AuralObj [S], NuagesObj[S]]
  //  protected final val viewToAuralMap  = TMap.empty[NuagesObj[S], AuralObj [S]]

  final def nuages(implicit tx: S#Tx): Nuages[S] = nuagesH()

  private[this] var  _keyControl: Control with Disposable[S#Tx] = _
  protected final def keyControl: Control with Disposable[S#Tx] = _keyControl

  override protected def guiInit(): Unit = {
    super.guiInit()
  }

  final def init(repr: Repr)(implicit tx: S#Tx): this.type = {
    _keyControl = KeyControl(main)
    deferTx(guiInit())
    initObservers(repr)
    this
  }

  final def dispose()(implicit tx: S#Tx): Unit = {
    disposeTransport()
    disposeNodes()
    deferTx(stopAnimation())
    clearSolo()
    observers.foreach(_.dispose())
    disposeAuralObserver()
    transport.dispose()
    //    auralToViewMap.foreach { case (_, vp) =>
    //      vp.dispose()
    //    }
    //    viewToAuralMap.clear()
    //    auralToViewMap.clear()
    // scanMap       .dispose()
    //    missingScans  .dispose()

    keyControl    .dispose()
  }

  protected final def disposeAuralObserver()(implicit tx: S#Tx): Unit = {
    auralReprRef() = None
    auralObserver.swap(None).foreach(_.dispose())
  }

  final def selection: Set[NuagesNode[S]] = {
    requireEDT()
    val selectedItems = visualization.getGroup(GROUP_SELECTION)
    import scala.collection.JavaConversions._
    selectedItems.tuples().flatMap {
      case ni: NodeItem =>
        ni.get(COL_NUAGES) match {
          case vn: NuagesNode[S] => Some(vn)
          case _ => None
        }
      case _ => None
    } .toSet
  }

  private[this] val guiCode = TxnLocal(init = Vector.empty[() => Unit], afterCommit = handleGUI)

  private[this] def handleGUI(seq: Vec[() => Unit]): Unit = {
    def exec(): Unit = visualization.synchronized {
      stopAnimation()
      // AGGR_LOCK = true
      seq.foreach { fun =>
        try {
          fun()
        } catch {
          case NonFatal(e) => e.printStackTrace()
        }
      }
      // AGGR_LOCK = false
      startAnimation()
    }

    defer(exec())
  }

  final def deferVisTx(thunk: => Unit)(implicit tx: TxnLike): Unit =
    guiCode.transform(_ :+ (() => thunk))

  @inline private[this] def stopAnimation(): Unit = {
    import MeshPanelImpl.{ACTION_COLOR, ACTION_LAYOUT}
    visualization.cancel(ACTION_COLOR)
    visualization.cancel(ACTION_LAYOUT)
  }

  @inline private[this] def startAnimation(): Unit = {
    import MeshPanelImpl.ACTION_COLOR
    visualization.run(ACTION_COLOR)
  }

  protected final def getAuralScanData(aural: AuralObj[S], key: String = Proc.mainOut)
                                      (implicit tx: S#Tx): Option[(AudioBus, Node)] = aural match {
    case ap: AuralObj.Proc[S] =>
      None // SCAN
    //      val d = ap.data
    //      for {
    //        either  <- d.getScanOut(key)
    //        nodeRef <- d.nodeOption
    //      } yield {
    //        val bus   = either.fold(identity, _.bus)
    //        val node  = nodeRef.node
    //        (bus, node)
    //      }
    case _ => None
  }

  // private def close(p: Container): Unit = p.peer.getParent.remove(p.peer)

  final def saveMacro(name: String, sel: Set[NuagesObj[S]]): Unit =
    cursor.step { implicit tx =>
      val copies = Nuages.copyGraph(sel.map(_.obj)(breakOut))

      val macroF = Folder[S]
      copies.foreach(macroF.addLast)
      val nuagesF = panel.nuages.folder
      import proc.Implicits._
      val parent = nuagesF.iterator.collect {
        case parentObj: Folder[S] if parentObj.name == Nuages.NameMacros => parentObj
      } .toList.headOption.getOrElse {
        val res = Folder[S]
        res.name = Nuages.NameMacros
        nuagesF.addLast(res)
        res
      }

      macroF.name = name
      parent.addLast(macroF)
    }
}