/*
 *  ConvolveFSc.scala
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
import de.sciss.fscape.gui.SimpleGUI
import de.sciss.fscape.stream.Control
import de.sciss.fscape.{GE, Graph, graph}

import scala.Predef.{any2stringadd => _, _}
import scala.swing.Swing

object ConvolveFSc {
  final case class Config(kernel: Int = 16, noiseAmp: Double = 0.1, width: Int = 1024, height: Int = 1024)

  def main(args: Array[String]): Unit = run(Config())

  def run(config: Config): Unit = {
    import config._
    val dir       = userHome / "Documents" / "projects" / "Imperfect" / "scans" /"notebook2016"
    val fIn1      = dir / "universe-test1.png"
    val fIn2      = dir / "universe-test2.png"
    val fFltIn    = dir / s"hp5-fft2d-$kernel.aif"
    val fOut      = dir / "universe-fscape-out.png"

    if (fOut.exists() && fOut.length() > 0) {
      println(s"File '${fOut.name}' already exists. Not overwriting")
      sys.exit(1)
    }

    var gui: SimpleGUI = null
    val cfg = Control.Config()
    cfg.useAsync = false
    cfg.progressReporter = p => Swing.onEDT(gui.progress = p.total)

    val g = Graph {
      import graph._
      val i1        = ImageFileIn(fIn1, numChannels = 3)
      val i2        = ImageFileIn(fIn2, numChannels = 3)
      val frameSize = width * height

      val fltIn     = AudioFileIn(fFltIn, numChannels = 1)  // already FFT'ed
      val kernelS   = kernel * kernel
      val fltRepeat = RepeatWindow(fltIn, kernelS, num = frameSize)

      val m1        = MatrixInMatrix(i1, rowsOuter = height, columnsOuter = width, rowsInner = kernel, columnsInner = kernel)
      val m2        = MatrixInMatrix(i2, rowsOuter = height, columnsOuter = width, rowsInner = kernel, columnsInner = kernel)

      val m1a       = AffineTransform2D.scale(in = m1, widthIn = kernel, heightIn = kernel,
        sx = 0.2, sy = 0.2, zeroCrossings = 0, wrap = 0)

      val m1n       = ResizeWindow(WhiteNoise(Seq[GE](1, 1, 1)), size = 1, start = 0, stop = kernelS - 1)
      val m1x       = m1a + (m1n * 24 + 104)

      val m1f       = Real2FFT(m1x, rows = kernel, columns = kernel)
      val m2f       = Real2FFT(m2 , rows = kernel, columns = kernel)

      val m3f       = (m1f.complex * m2f).complex * fltRepeat
      val m3        = Real2IFFT(m3f, rows = kernel, columns = kernel)
      val flt       = ResizeWindow(m3, size = kernelS, stop = -(kernelS - 1))
      val i3        = flt

      Progress(Frames(i3) / (2 * frameSize), Metro(width), label = "ifft")

      val frameTr1  = Metro(frameSize)
      val frameTr2  = Metro(frameSize)
      val maxR      = RunningMax(i3, trig = frameTr1).drop(frameSize - 1)
      val minR      = RunningMin(i3, trig = frameTr1).drop(frameSize - 1)
      val max       = Gate(maxR, gate = frameTr2)
      val min       = Gate(minR, gate = frameTr2)
      val mul       = (max - min).reciprocal
      val add       = -min
      val i3e       = i3.elastic(frameSize / cfg.blockSize + 1)
      val noise     = WhiteNoise(noiseAmp)
      val i4        = ((i3e + add) * mul + noise).max(0).min(1)

      val sig       = i4
      val specOut   = ImageFile.Spec(width = width, height = height, numChannels = 3)
      ImageFileOut(fOut, specOut, in = sig)
      Progress(Frames(sig) / (2 * frameSize), Metro(frameSize), label = "write")
    }

    val ctl = Control(cfg)
    Swing.onEDT {
      gui = SimpleGUI(ctl)
    }
    ctl.run(g)
  }
}
