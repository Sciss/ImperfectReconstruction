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
  final case class Config(kernel: Int = 16, noiseAmp: Double = 0.1, width: Int = 1024, height: Int = 1024,
                          groupIdx: Int = 1, fadeFrames: Int = 24 /* * 14 */)

  def main(args: Array[String]): Unit = run(Config())

  case class Levels(r: (Int, Int), g: (Int, Int), b: (Int, Int), value: (Int, Int))

  def applyLevels(in: GE, levels: Levels): GE = {
    import levels._
    val chans = r :: g :: b :: Nil
    val rgb   = in // Seq(in \ 0, in \ 1, in \ 2): GE
//    val alpha = in \ 3
    val low   = chans.map(tup => tup._1 / 255.0: GE)
    val high  = chans.map(tup => tup._2 / 255.0: GE)
    val p1    = rgb.linlin(low, high, 0, 1)
    val p2    = p1 .linlin(value._1 / 255.0, value._2 / 255.0, 0, 1)
    val clip  = p2 .max(0).min(1).elastic(10)
    clip // Seq(clip \ 0, clip \ 1, clip \ 2 /* , alpha */): GE
  }

  def invert(in: GE): GE = (-in + (1: GE)).elastic(2)

  def run(config: Config): Unit = {
    import config._
    val baseDir       = userHome / "Documents" / "projects" / "Imperfect" / "scans" /"notebook2016"
    val pagesDir      = baseDir / "universe-pages"
    val groupInDir    = pagesDir / s"group-$groupIdx"
    val numPages      = groupInDir.children(_.ext == "png").size
    val outDir        = baseDir / "universe-out"
    val groupOutDir   = outDir / s"group-$groupIdx"
    groupOutDir.mkdirs()
    val tempIn        = groupInDir  / "notebook-p%d.png"
    val tempOut       = groupOutDir / "frame-%d.png"
    val tempInRange   = 1 to numPages
    val numFrames     = (2 * numPages + 1) * fadeFrames
    // val tempOutRange  = 1 to numFrames

    val fFltIn    = baseDir / s"hp5-fft2d-$kernel.aif"

//    if (fOut.exists() && fOut.length() > 0) {
//      println(s"File '${fOut.name}' already exists. Not overwriting")
//      sys.exit(1)
//    }

    var gui: SimpleGUI = null
    val cfg = Control.Config()
    cfg.useAsync          = false
    cfg.blockSize         = 8192
    cfg.progressReporter  = p => Swing.onEDT(gui.progress = p.total)

    def mkRangeGE(in: Seq[Int]): GE = in.map(x => x: GE).reduce(_ ++ _)

    val lvl = Levels(r = 41 -> 254, g = 41 -> 254, b = 41 -> 254, value = 0 -> 200)

    def adjustLevels(in: GE): GE = invert(applyLevels(in, lvl))

    val g = Graph {
      import graph._
      val frameSize   = width.toLong * height
      val (evenInRange, oddInRange) = tempInRange.partition(_ % 2 == 0)

      def mkBlackFrames(num: Int): GE = DC(Seq[GE](0, 0, 0)).take(frameSize * num)

      val imgSeqIn1   = ImageFileSeqIn(tempIn, indices = mkRangeGE(evenInRange), numChannels = 3)
      val imgSeqIn2   = ImageFileSeqIn(tempIn, indices = mkRangeGE(oddInRange ), numChannels = 3)
      val imgSeqIn1L  = adjustLevels(imgSeqIn1)
      val imgSeqIn2L  = adjustLevels(imgSeqIn2)

      val (inSeqRep1, inSeqRep2) = if (evenInRange.size < oddInRange.size) {
        val _res1 = mkBlackFrames(fadeFrames) ++ RepeatWindow(imgSeqIn1L, size = frameSize, num = 4 * fadeFrames) ++
          mkBlackFrames(fadeFrames)
        val _res2 = RepeatWindow(imgSeqIn2L, size = frameSize, num = 4 * fadeFrames).drop(frameSize * fadeFrames)
          .take(frameSize * (oddInRange.size * 4 - 2) * fadeFrames)
        (_res1, _res2)
      } else {
        ???
      }

      val fltIn     = AudioFileIn(fFltIn, numChannels = 1)  // already FFT'ed
      val kernelS   = kernel * kernel
      val fltRepeat = RepeatWindow(fltIn, kernelS, num = frameSize * numFrames)

      val periodFrames = fadeFrames * 4

      def mkSaw(phase: Double): GE = {
        val lfSaw   = LFSaw(1.0/periodFrames, phase = phase)
        val lfSawUp = (lfSaw + (1: GE)) * 2
        val low     = lfSawUp.min(1) - (lfSawUp - 3).max(0)
        val rep1    = RepeatWindow(low , size = 1, num = frameSize)
        rep1
      }

      def mkSawMat(phase: Double): GE = {
        val rep1    = mkSaw(phase)
        val rep2    = RepeatWindow(rep1, size = 1, num = kernelS  )
        rep2
      }
      
      val env1Mat   = mkSawMat(0.75)
      val env2Mat   = mkSawMat(0.25)

      val m1        = MatrixInMatrix(inSeqRep1, rowsOuter = height, columnsOuter = width, rowsInner = kernel, columnsInner = kernel)
      val m2        = MatrixInMatrix(inSeqRep2, rowsOuter = height, columnsOuter = width, rowsInner = kernel, columnsInner = kernel)

//      m1.poll(Metro(frameSize/8), "m1")
//      m2.poll(Metro(frameSize/8), "m2")

      val scale1    = env1Mat.linexp(1, 0, 1, 0.01)
      val scale2    = env2Mat.linexp(1, 0, 1, 0.01)
      val m1a       = AffineTransform2D.scale(in = m1, widthIn = kernel, heightIn = kernel,
        sx = scale1, sy = scale1, zeroCrossings = 0, wrap = 0)
      val m2a       = AffineTransform2D.scale(in = m2, widthIn = kernel, heightIn = kernel,
        sx = scale2, sy = scale2, zeroCrossings = 0, wrap = 0)
//      val m1a = m1
//      val m2a = m2

      // (m1a \ 0).poll(Metro(frameSize/8), "m1a")
      val m1ab      = m1a // BufferDisk(m1a)
      val m2ab      = m2a // BufferDisk(m2a)

      val env1      = mkSaw(0.75)
      val env2      = mkSaw(0.25)

      val env1p     = env1.pow(4)
      val env2p     = env2.pow(4)
      val noiseAmp1 = env1p.linlin(1, 0, 0,  24)
      val noiseDC1  = env1p.linlin(1, 0, 0, 104)
      val noiseAmp2 = env2p.linlin(1, 0, 0,  24)
      val noiseDC2  = env2p.linlin(1, 0, 0, 104)

      val noise1    = WhiteNoise(Seq[GE](noiseAmp1, noiseAmp1, noiseAmp1)) + noiseDC1
      val m1n       = ResizeWindow(noise1, size = 1, start = 0, stop = kernelS - 1)
      val m1x       = m1ab + m1n // (m1n * 24 + (104: GE))

      val noise2    = WhiteNoise(Seq[GE](noiseAmp2, noiseAmp2, noiseAmp2)) + noiseDC2
      val m2n       = ResizeWindow(noise2, size = 1, start = 0, stop = kernelS - 1)
      val m2x       = m2ab + m2n // (m1n * 24 + (104: GE))

      val m1f       = Real2FFT(m1x, rows = kernel, columns = kernel)
      val m2f       = Real2FFT(m2x, rows = kernel, columns = kernel)

      val m3f       = (m1f.complex * m2f).complex * fltRepeat
      val m3        = Real2IFFT(m3f, rows = kernel, columns = kernel)
      val flt       = ResizeWindow(m3, size = kernelS, stop = -(kernelS - 1))
      val i3        = flt
//      val i3        = BufferDisk(flt)

//      BufferDisk(m1f \ 0).poll(Metro(frameSize/8), "m1f")
//      BufferDisk(flt \ 0).poll(Metro(frameSize/8), "flt")
      (flt \ 0).poll(Metro(frameSize/8), "flt")

      //      Progress(Frames(i3) / (2 * frameSize), Metro(width), label = "ifft")

      val frameTr1  = Metro(frameSize)
      // val frameTr2  = Metro(frameSize)
      val maxR      = RunningMax(i3, trig = frameTr1).drop(frameSize - 1)
      val minR      = RunningMin(i3, trig = frameTr1).drop(frameSize - 1)
      val maxRDec   = ResizeWindow(maxR, frameSize, start = 0, stop = -(frameSize - 1))
      val minRDec   = ResizeWindow(minR, frameSize, start = 0, stop = -(frameSize - 1))
      val maxLag    = OnePole(maxRDec, 1 - 1.0 / 24)
      val minLag    = OnePole(minRDec, 1 - 1.0 / 24)
      val mul       = (maxLag - minLag).max(0.05).reciprocal
      val add       = -minLag.elastic(3)
      val mulR      = RepeatWindow(mul, size = 1, num = frameSize)
      val addR      = RepeatWindow(add, size = 1, num = frameSize)
      val i3e       = i3.elastic(frameSize * 6 / cfg.blockSize + 1)
//      val i3e       = BufferDisk(i3)
      val noise     = WhiteNoise(noiseAmp)

      (addR \ 0).poll(Metro(frameSize), "add")
      (mulR \ 0).poll(Metro(frameSize), "mul")

      val i4        = ((i3e + addR) * mulR + noise).max(0).min(1)

      (i4  \ 0).poll(Metro(frameSize/8), "i4")

      val sig       = i4
      val specOut   = ImageFile.Spec(width = width, height = height, numChannels = 3)
      val tempOutRangeGE = Frames(DC(0).take(numFrames))
      ImageFileSeqOut(tempOut, spec = specOut, in = sig, indices = tempOutRangeGE)
      Progress(Frames(sig \ 0) / (frameSize.toLong * numFrames), Metro(frameSize), label = "write")
    }

    val ctl = Control(cfg)
    Swing.onEDT {
      gui = SimpleGUI(ctl)
    }
    ctl.run(g)
  }
}