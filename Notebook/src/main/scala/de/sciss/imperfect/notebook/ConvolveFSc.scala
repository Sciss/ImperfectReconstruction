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
import de.sciss.numbers

import scala.Predef.{any2stringadd => _, _}
import scala.swing.Swing

object ConvolveFSc {
  final case class Config(kernel: Int = 16, noiseAmp: Double = 0.05, width: Int = 1024, height: Int = 1024,
                          groupIdx: Int = 5, fadeFrames: Int = 24 * 2 /* * 14 */, skipFrames: Int = 0,
                          lagTime: Double = 1.0 - 1.0/24)

  def main(args: Array[String]): Unit = run(Config())

  case class Levels(r: (Int, Int), g: (Int, Int), b: (Int, Int), value: (Int, Int))

  private[this] final val DEBUG = false

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

      val (inSeqRep1a, inSeqRep2a) = if (evenInRange.size < oddInRange.size) {
        val _res1 = mkBlackFrames(fadeFrames) ++ RepeatWindow(imgSeqIn1L, size = frameSize, num = 4 * fadeFrames) ++
          mkBlackFrames(fadeFrames)
        val _res2 = RepeatWindow(imgSeqIn2L, size = frameSize, num = 4 * fadeFrames).drop(frameSize * fadeFrames)
          .take(frameSize * (oddInRange.size * 4 - 2) * fadeFrames)
        (_res1, _res2)
      } else {
        ???
      }
      val inSeqRep1 = if (skipFrames == 0) inSeqRep1a else inSeqRep1a.drop(skipFrames * frameSize)
      val inSeqRep2 = if (skipFrames == 0) inSeqRep2a else inSeqRep2a.drop(skipFrames * frameSize)
      val numFramesS = numFrames - skipFrames

      val fltIn     = AudioFileIn(fFltIn, numChannels = 1)  // already FFT'ed
      val kernelS   = kernel * kernel
      val fltRepeat = RepeatWindow(fltIn, size = kernelS, num = frameSize * numFramesS)

      val periodFrames = fadeFrames * 4

      def mkSawLow(phase: Double): GE = {
        val lfSaw   = LFSaw(1.0/periodFrames, phase = phase)
        val lfSawUp = (lfSaw + (1: GE)) * 2
        val low0    = lfSawUp.min(1) - (lfSawUp - 3).max(0)
        val low     = if (skipFrames == 0) low0 else low0.drop(skipFrames)
        low
      }

      def mkSaw(phase: Double): GE = {
        val low     = mkSawLow(phase)
        val rep1    = RepeatWindow(low , size = 1, num = frameSize)
        rep1
      }

      def mkSawMat(phase: Double): GE = {
        val rep1    = mkSaw(phase)
        val rep2    = RepeatWindow(rep1, size = 1, num = kernelS  )
        rep2
      }
      
      val env1Mat   = mkSawLow(0.75)
      val env2Mat   = mkSawLow(0.25)

      val m1        = MatrixInMatrix(inSeqRep1, rowsOuter = height, columnsOuter = width, rowsInner = kernel, columnsInner = kernel)
      val m2        = MatrixInMatrix(inSeqRep2, rowsOuter = height, columnsOuter = width, rowsInner = kernel, columnsInner = kernel)

//      val scale1l   = env1Mat.linexp(1, 0, 1, 0.5/kernel /* 0.01 */)
//      val scale2l   = env2Mat.linexp(1, 0, 1, 0.5/kernel /* 0.01 */)
      import numbers.Implicits._
      val scale1l   = (env1Mat * 8 - 4).atan.linlin(-4.0.atan: GE, 4.0.atan: GE, 0.5/kernel: GE, 1: GE)
      val scale2l   = (env2Mat * 8 - 4).atan.linlin(-4.0.atan: GE, 4.0.atan: GE, 0.5/kernel: GE, 1: GE)
//      val scale1l   = env1Mat.pow(2)
//      val scale2l   = env2Mat.pow(2)
      val ampMat1l  = env1Mat // .pow(1.0/8)
      val ampMat2l  = env2Mat // .pow(1.0/8)
      val ampMat1   = RepeatWindow(ampMat1l, size = 1, num = frameSize * kernelS)
      val ampMat2   = RepeatWindow(ampMat2l, size = 1, num = frameSize * kernelS)
      val scale1    = RepeatWindow(scale1l , size = 1, num = frameSize * kernelS)
      val scale2    = RepeatWindow(scale2l , size = 1, num = frameSize * kernelS)
      val m1a       = AffineTransform2D.scale(in = m1, widthIn = kernel, heightIn = kernel,
        sx = scale1, sy = scale1, zeroCrossings = 0, wrap = 0) * ampMat1
      val m2a       = AffineTransform2D.scale(in = m2, widthIn = kernel, heightIn = kernel,
        sx = scale2, sy = scale2, zeroCrossings = 0, wrap = 0) * ampMat2
//      val m1a = m1
//      val m2a = m2

      // (m1a \ 0).poll(Metro(frameSize/8), "m1a")
      val m1ab      = m1a // BufferDisk(m1a)
      val m2ab      = m2a // BufferDisk(m2a)

      val env1      = mkSawLow(0.75)
      val env2      = mkSawLow(0.25)

      val env1p     = env1.pow(8)
      val env2p     = env2.pow(8)
//      val noiseAmp1l= (-env1p + (0.9: GE)).max(0) // 1 - env1p  // env1p.linlin(1, 0, 0, 1 /* 24 */)
      val noiseAmp1l= env1p.linlin(0, 1, 1.0, -0.15).max(0) // 1 - env1p  // env1p.linlin(1, 0, 0, 1 /* 24 */)
      val noiseDC1l = noiseAmp1l * 2 // env1p.linlin(1, 0, 0, 1 /* 24 */ /* 104 */)
//      val noiseAmp2l= (-env2p + (0.9: GE)).max(0) // 1 - env2p  // env2p.linlin(1, 0, 0, 1 /* 24 */)
      val noiseAmp2l= env2p.linlin(0, 1, 1.0, -0.15).max(0) // 1 - env1p  // env1p.linlin(1, 0, 0, 1 /* 24 */)
      val noiseDC2l = noiseAmp2l * 2 // env2p.linlin(1, 0, 0, 1 /* 24 */ /* 104 */)
      val noiseAmp1 = RepeatWindow(noiseAmp1l, size = 1, num = frameSize)
      val noiseAmp2 = RepeatWindow(noiseAmp2l, size = 1, num = frameSize)
      val noiseDC1  = RepeatWindow(noiseDC1l , size = 1, num = frameSize)
      val noiseDC2  = RepeatWindow(noiseDC2l , size = 1, num = frameSize)

      if (DEBUG) {
        noiseAmp1.poll(Metro(frameSize), "noiseAmp1")
        noiseAmp2.poll(Metro(frameSize), "noiseAmp2")
        noiseDC1 .poll(Metro(frameSize), "noiseDC1")
        noiseDC2 .poll(Metro(frameSize), "noiseDC2")
      }

      val noise1    = WhiteNoise(Seq[GE](noiseAmp1, noiseAmp1, noiseAmp1)) + noiseDC1
      val noise2    = WhiteNoise(Seq[GE](noiseAmp2, noiseAmp2, noiseAmp2)) + noiseDC2

      // val m1n       = ResizeWindow(noise1, size = 1, start = 0, stop = kernelS - 1)
      val m1n       = ResizeWindow(noise1, size = 1, start = -kernelS/2, stop = kernelS/2 - 1)
      // val m2n       = ResizeWindow(noise2, size = 1, start = 0, stop = kernelS - 1)
      val m2n       = ResizeWindow(noise2, size = 1, start = -kernelS/2, stop = kernelS/2 - 1)

      if (DEBUG) {
        m1n.poll(Metro(frameSize * kernelS), "m1n")
        m2n.poll(Metro(frameSize * kernelS), "m2n")
      }

      val m1x       = m1ab + m1n // (m1n * 24 + (104: GE))
      val m2x       = m2ab + m2n // (m1n * 24 + (104: GE))

      val m1f       = Real2FFT(m1x, rows = kernel, columns = kernel)
      val m2f       = Real2FFT(m2x, rows = kernel, columns = kernel)

      val m3f       = (m1f.complex * m2f).complex * fltRepeat
      val m3        = Real2IFFT(m3f, rows = kernel, columns = kernel)
      val flt       = ResizeWindow(m3, size = kernelS, stop = -(kernelS - 1))
      val i3        = flt

      val noise     = WhiteNoise(noiseAmp)
      val i3g       = ARCWindow(i3, size = frameSize, lag = lagTime)
      val i4        = (i3g + noise).max(0.0).min(1.0)

      (i4 \ 0).poll(Metro(frameSize), "frame-done")

      val sig       = i4
      val specOut   = ImageFile.Spec(width = width, height = height, numChannels = 3)
      val tempOutRangeGE0 = Frames(DC(0).take(numFramesS))
      val tempOutRangeGE = if (skipFrames == 0) tempOutRangeGE0 else tempOutRangeGE0 + (skipFrames: GE)
      ImageFileSeqOut(tempOut, spec = specOut, in = sig, indices = tempOutRangeGE)
      Progress(Frames(sig \ 0) / (frameSize.toLong * numFramesS), Metro(frameSize), label = "write")
    }

    val ctl = Control(cfg)
    Swing.onEDT {
      gui = SimpleGUI(ctl)
    }
    ctl.run(g)
  }
}