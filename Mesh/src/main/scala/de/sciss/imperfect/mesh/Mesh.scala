package de.sciss.imperfect.mesh

import de.sciss.lucre.stm
import de.sciss.lucre.synth.InMemory
import de.sciss.nuages.ScissProcs.NuagesFinder
import de.sciss.nuages.{Nuages, NuagesFrame, NuagesView, ScissProcs, Wolkenpumpe}
import de.sciss.osc
import de.sciss.synth.proc.Action.Universe
import de.sciss.synth.proc.{AuralSystem, Folder}
import de.sciss.synth.{Server => SServer}

object Mesh {
  def main(args: Array[String]): Unit = run()

  def run(): Unit = {
    type S = InMemory
    implicit val cursor = InMemory()

    object Pompe extends Wolkenpumpe[S] {
      override def configure(sCfg: ScissProcs.ConfigBuilder, nCfg: Nuages.ConfigBuilder,
                              aCfg: SServer.ConfigBuilder): Unit = super.configure(sCfg, nCfg, aCfg)

      override def registerProcesses(sCfg: ScissProcs.Config, nCfg: Nuages.Config, nuagesFinder: NuagesFinder)
                                     (implicit tx: S#Tx, cursor: stm.Cursor[S], nuages: Nuages[S],
                                      aural: AuralSystem): Unit =
        super.registerProcesses(sCfg, nCfg, nuagesFinder)
    }

    // Submin.install(true)
    Wolkenpumpe.init()

    val nCfg                = Nuages    .Config()
    val sCfg                = ScissProcs.Config()
    val aCfg                = SServer   .Config()

    nCfg.recordPath         = Option(sys.props("java.io.tmpdir"))
    aCfg.deviceName         = Some("Wolkenpumpe")
    aCfg.audioBusChannels   = 512
    aCfg.memorySize         = 256 * 1024
    aCfg.transport          = osc.TCP
    aCfg.pickPort()

    Pompe.configure(sCfg, nCfg, aCfg)

    val maxInputs   = ((sCfg.lineInputs ++ sCfg.micInputs).map(_.stopOffset) :+ 0).max
    val maxOutputs  = (
      sCfg.lineOutputs.map(_.stopOffset) :+ nCfg.soloChannels.fold(0)(_.max + 1) :+ nCfg.masterChannels.fold(0)(_.max + 1)
      ).max
    println(s"numInputs = $maxInputs, numOutputs = $maxOutputs")

    aCfg.outputBusChannels  = maxOutputs
    aCfg.inputBusChannels   = maxInputs

    cursor.step { implicit tx =>
      implicit val aural = AuralSystem()
      //_aural = aural

      val f = Folder[S]
      val surface = Nuages.Surface.Folder[S](f)
      implicit val nuages = Nuages[S](surface)
      val nuagesH = tx.newHandle(nuages)

      val finder = new NuagesFinder {
        def findNuages[T <: stm.Sys[T]](universe: Universe[T])(implicit tx: T#Tx): Nuages[T] = {
          nuagesH.asInstanceOf[stm.Source[T#Tx, Nuages[T]]]()
        }
      }

      Pompe.registerProcesses(sCfg, nCfg, finder)

      import de.sciss.synth.proc.WorkspaceHandle.Implicits._
      val _view = MeshView(nuages, nCfg, sCfg)
      /* val frame = */ MeshFrame(_view, undecorated = false /* true */)
      aural.start(aCfg)
    }
  }
}