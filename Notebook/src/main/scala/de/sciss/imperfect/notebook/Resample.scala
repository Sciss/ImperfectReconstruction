/*
 *  Resample.scala
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

import javax.imageio.ImageIO

import de.sciss.file._
import de.sciss.fscape.{GE, Graph}
import de.sciss.fscape.graph
import de.sciss.fscape.gui.SimpleGUI
import de.sciss.fscape.stream.Control
import de.sciss.numbers.Implicits._

import scala.Predef.{any2stringadd => _, _}
import scala.swing.Swing

object Resample {
  final case class Config(tempIn: File = file("in-%d.png"), tempOut: File = file("out-%d.png"),
                          startIndex: Int = 1, endIndex0: Int = 0, factor: Double = 4,
                          noiseAmt: Double = 0.05, gamma: Double = 0.8)

  def main(args: Array[String]): Unit = {
    val p = new scopt.OptionParser[Config]("Imperfect-Notebook Convolve") {
      opt[File]('i', "input")
        .text ("Input template")
        .required()
        .action { (f, c) => c.copy(tempIn = f) }

      opt[File]('o', "output")
        .text ("Output template")
        .required()
        .action { (f, c) => c.copy(tempOut = f) }

      opt[Int] ('s', "start-index")
        .text ("Start frame index")
        .action   { (v, c) => c.copy(startIndex = v) }
        .validate {  v     => if (v >= 0) success else failure("start-frame must be >= 0") }

      opt[Int] ('e', "end-index")
        .text ("End frame index")
        .action   { (v, c) => c.copy(endIndex0 = v) }
        .validate {  v     => if (v >= 0) success else failure("end-frame must be >= 0") }

      opt[Double] ('g', "gamma")
        .text ("Gamma correction (< 1 darker, > 1 brighter, default: 0.5")
        .action   { (v, c) => c.copy(gamma = v) }
        .validate { v => if (v > 0) success else failure("gamma must be > 0") }

      opt[Double] ('f', "factor")
        .required()
        .text ("Resampling factor")
        .action   { (v, c) => c.copy(factor = v) }
    }
    p.parse(args, Config()).fold(sys.exit(1)) { config =>
      run(config)
    }
  }

  def format(f: File, i: Int): File = {
    val n = f.name.format(i)
    f.parentOption.fold(file(n))(_ / n)
  }

  def run(config: Config): Unit = {
    import config._
    var gui: SimpleGUI = null
    val cfg = Control.Config()
    cfg.useAsync          = false
    cfg.progressReporter  = p => Swing.onEDT(gui.progress = p.total)

    val imgTest   = ImageIO.read(format(tempIn, startIndex))
    val width     = imgTest.getWidth
    val height    = imgTest.getHeight
    imgTest.flush()

    val endIndex  = if (endIndex0 >= startIndex) endIndex0 else
      Iterator.from(startIndex).takeWhile(i => format(tempIn, i).isFile).toList.last
    val numFramesIn = endIndex - startIndex + 1
    val numFramesOut = math.ceil(numFramesIn * factor).toInt

    val g = Graph {
      import graph._
      val frameSize   = width.toLong * height
      val indicesIn   = Line(startIndex, endIndex, endIndex - startIndex + 1)
      val indicesOut  = Line(1, numFramesOut, numFramesOut)
      val in          = ImageFileSeqIn(tempIn, numChannels = 3, indices = indicesIn)
      val r           = ResampleWindow(in, size = frameSize, factor = factor, kaiserBeta = Seq[GE](6.5, 7.5, 8.5))
      val n           = if (noiseAmt <= 0) r else  r + WhiteNoise(noiseAmt)
      val g           = if (gamma == 1)    n else n.pow(gamma.reciprocal)
      val sig         = g.max(0).min(1)
      val fileType    = if (tempOut.ext.toLowerCase == "png") ImageFile.Type.PNG else ImageFile.Type.JPG
      val specOut     = ImageFile.Spec(width = width, height = height, numChannels = 3, fileType = fileType)
      ImageFileSeqOut(tempOut, specOut, indices = indicesOut, in = sig)
      Progress(Frames(sig \ 0) / (frameSize * numFramesOut), Metro(frameSize))
    }

    val ctl = Control(cfg)
    Swing.onEDT {
      gui = SimpleGUI(ctl)
    }
    ctl.run(g)
  }
}
