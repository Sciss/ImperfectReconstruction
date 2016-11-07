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
                          noiseAmt: Double = 0.05, gamma: Double = 0.8,
                          kaiserBeta: Seq[Double] = Seq(6.5, 7.5, 8.5),
                          rollOff: Seq[Double] = Seq(0.86),
                          zeroCrossings: Seq[Int] = Seq(15))

  def main(args: Array[String]): Unit = {
    val p = new scopt.OptionParser[Config]("Imperfect-Notebook Resample") {
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
        .text ("Gamma correction (< 1 darker, > 1 brighter, default: 0.8")
        .action   { (v, c) => c.copy(gamma = v) }
        .validate { v => if (v > 0) success else failure("gamma must be > 0") }

      opt[Double] ('f', "factor")
        .required()
        .text ("Resampling factor")
        .action   { (v, c) => c.copy(factor = v) }

      opt[Seq[Double]] ("kaiser-beta")
        .text ("List of Kaiser window beta values (up to three); default: 6.5,7.5,8.5")
        .validate {  v     =>
          if (v.size > 3) failure("can only take up to three values")
          else if (v.exists(x => x < 0)) failure("must be >= 0")
          else success
        }
        .action   { (v, c) => c.copy(kaiserBeta = v) }

      opt[Seq[Double]] ("roll-off")
        .text ("List of filter roll off values (up to three); default: 0.86")
        .validate {  v     =>
          if (v.size > 3) failure("can only take up to three values")
          else if (v.exists(x => x < 0 || x > 1)) failure("must be >= 0 and <= 1")
          else success
        }
        .action   { (v, c) => c.copy(rollOff = v) }

      opt[Seq[Int]] ("zero-crossings")
        .text ("List of sinc zero crossing values (up to three); default: 15")
        .validate {  v     => if (v.size <= 3) success else failure("can only take up to three values") }
        .action   { (v, c) => c.copy(zeroCrossings = v) }

      opt[Double] ("noise")
        .text ("Amount of noise; default: 0.05")
        .action   { (v, c) => c.copy(noiseAmt = v) }
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
    cfg.blockSize         = 8192
    cfg.progressReporter  = p => Swing.onEDT(gui.progress = p.total)

    val imgTest   = ImageIO.read(format(tempIn, startIndex))
    val width     = imgTest.getWidth
    val height    = imgTest.getHeight
    imgTest.flush()

    val endIndex  = if (endIndex0 >= startIndex) endIndex0 else
      Iterator.from(startIndex).takeWhile(i => format(tempIn, i).isFile).toList.last
    val numFramesIn = endIndex - startIndex + 1
    val numFramesOut = math.ceil(numFramesIn * factor).toInt

    tempOut.parentOption.foreach(_.mkdirs())

    val g = Graph {
      import graph._
      val frameSize       = width.toLong * height
      val indicesIn       = Line(startIndex, endIndex, endIndex - startIndex + 1)
      val indicesOut      = Line(1, numFramesOut, numFramesOut)
      val in              = ImageFileSeqIn(tempIn, numChannels = 3, indices = indicesIn)
      val kaiserBetaGE    = kaiserBeta   .map(x => x: GE)
      val rollOffGE       = rollOff      .map(x => x: GE)
      val zeroCrossingsGE = zeroCrossings.map(x => x: GE)
      val r0              = ResampleWindow(in, size = frameSize, factor = factor,
        kaiserBeta = kaiserBetaGE, rollOff = rollOffGE, zeroCrossings = zeroCrossingsGE)

      val fltSmpPerCrossing = 4096
      val zeroCrossingsP  = Vector.tabulate(3)(i => zeroCrossings(i % zeroCrossings.size))
      val rollOffP        = Vector.tabulate(3)(i => rollOff      (i % rollOff      .size))
      val fltLenH         = (zeroCrossingsP zip rollOffP).map { case (zc, ro) =>
        ((fltSmpPerCrossing * zc) / ro + 0.5).toInt
      }
      val minFltIncr      = fltSmpPerCrossing * math.min(1.0, factor)
      val maxFltLenH      = fltLenH.map { flh => math.round(math.ceil(flh / minFltIncr)).toInt }
      val PAD             = 1
      val winLen          = maxFltLenH.map { mflh => (mflh << 1) + PAD }
      // XXX TODO --- this doesn't make sense, should be (max - min) * 2; but needs more; why?
      val winLenD         = winLen.max // (winLen.max - winLen.min) * 2
      val r               = if (winLenD == 0) r0 else {
        val blocksDly = (frameSize * winLenD / cfg.blockSize) + 1
        println(s"To synchronize channels, need to buffer $winLenD frames or $blocksDly blocks.")
        r0.elastic(blocksDly)
      }

//      (r0 \ 0).poll(Metro(frameSize), "in -1")
//      (r0 \ 1).poll(Metro(frameSize), "in -2")
//      (r0 \ 2).poll(Metro(frameSize), "in -3")
//      (r  \ 0).poll(Metro(frameSize), "out-1")
//      (r  \ 1).poll(Metro(frameSize), "out-2")
//      (r  \ 2).poll(Metro(frameSize), "out-3")

      val n               = if (noiseAmt <= 0) r else  r + WhiteNoise(noiseAmt)
      val g               = if (gamma == 1)    n else n.pow(gamma.reciprocal)
      val sig             = g.max(0).min(1)
      val fileType        = if (tempOut.ext.toLowerCase == "png") ImageFile.Type.PNG else ImageFile.Type.JPG
      val specOut         = ImageFile.Spec(width = width, height = height, numChannels = 3, fileType = fileType)
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
