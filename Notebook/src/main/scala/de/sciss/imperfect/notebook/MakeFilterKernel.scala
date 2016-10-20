package de.sciss.imperfect.notebook

import de.sciss.file._
import de.sciss.fscape.{Graph, graph}
import de.sciss.fscape.gui.SimpleGUI
import de.sciss.fscape.stream.Control
import de.sciss.synth.io.{AudioFile, AudioFileSpec}

import scala.swing.Swing

object MakeFilterKernel extends App {
  val dir       = userHome / "Documents" / "projects" / "Imperfect" / "scans" /"notebook2016"
  val fIn       = dir / "hp5.aif"
  val kernel    = 16
  val fOut      = dir / s"hp5-fft2d-$kernel.aif"

  if (fOut.exists() && fOut.length() > 0) {
    println(s"File '${fOut.name}' already exists. Not overwriting")
    sys.exit(1)
  }

  var gui: SimpleGUI = _
  val cfg = Control.Config()
  cfg.useAsync = false
  cfg.progressReporter = p => Swing.onEDT(gui.progress = p.total)

  val inSpec = AudioFile.readSpec(fIn)

  val g = Graph {
    import graph._
    val in      = AudioFileIn(fIn, numChannels = 1)
    val kh      = kernel/2
    val inCrop  = in.drop((inSpec.numFrames - kernel)/2).take(kernel)
    val left    = inCrop.take(kh)
    val right   = inCrop.drop(kh)
    val rot     = right ++ left
    val inRep   = RepeatWindow(rot, size = kernel, num = kernel)
    val kernelS = kernel * kernel
    val inRepT  = TransposeMatrix(inRep, kernel, kernel)
    // Plot1D(inRepT, kernelS)
    val prod    = inRep * inRepT
    val fft     = Real2FFT(prod, kernel, kernel)
    val sig     = fft.take(kernelS) * kernelS/2
    val outSpec = AudioFileSpec(numChannels = 1, sampleRate = 44100)
    val written = AudioFileOut(fOut, outSpec, sig)
    Progress(written / kernelS, Metro(kernel))
  }

  val ctl = Control(cfg)
  Swing.onEDT {
    gui = SimpleGUI(ctl)
  }
  ctl.run(g)
}
