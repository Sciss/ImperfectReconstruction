/*
 *  MakeFilterKernel.scala
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

package de.sciss.imperfect.notebook

import de.sciss.file._
import de.sciss.fscape.{Graph, graph}
import de.sciss.fscape.gui.SimpleGUI
import de.sciss.fscape.stream.Control
import de.sciss.synth.io.{AudioFile, AudioFileSpec}

import scala.swing.Swing

object MakeFilterKernel {
  final case class Config(fIn: File = file("filter.aif"), fOut: File = file("filter-out.aif"),
                          kernel: Int = 16)

  def main(args: Array[String]): Unit = {
    val p = new scopt.OptionParser[Config]("Imperfect-Notebook Convolve") {
      opt[File] ('i', "input")
        .text ("Input one-dimensional filter sound file")
        .required()
        .action   { (v, c) => c.copy(fIn = v) }

      opt[File] ('o', "output")
        .text ("Two dimensional FFT'ed filter output sound file")
        .required()
        .action   { (v, c) => c.copy(fOut = v) }

      opt[Int] ('k', "kernel")
        .text ("Convolution kernel size. Must be a power of two. Default: 32")
        .action   { (v, c) => c.copy(kernel = v) }
    }
    p.parse(args, Config()).fold(sys.exit(1)) { config =>
      run(config)
    }
  }

  def run(config: Config): Unit = {
    import config._

    if (fOut.exists() && fOut.length() > 0) {
      println(s"File '${fOut.name}' already exists. Not overwriting")
      sys.exit(1)
    }

    var gui: SimpleGUI = null
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
}
