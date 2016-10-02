/*
 *  PositionWords.scala
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
import de.sciss.fscape.stream.Control
import de.sciss.fscape.{GE, Graph, graph}
import scopt.OptionParser

import scala.Predef.{any2stringadd => _, _}

object PositionWords {
  case class Config(inTemp   : File     = file("in-%.png"),
                    outTemp  : File     = file("out-%.png"),
                    overwrite: Boolean  = false
                   )

  def main(args: Array[String]): Unit = {
    val parser = new OptionParser[Config]("PositionWords") {
      opt[File  ]('i', "input-template")  required() text "Template in file" action { (x, c) => c.copy(inTemp  = x) }
      opt[File  ]('d', "output-template") required() text "Output template"  action { (x, c) => c.copy(outTemp = x) }
      opt[Unit  ]("overwrite") text "Overwrite existing files"  action { (_, c) => c.copy(overwrite = true) }
    }
    parser.parse(args, Config()).fold(sys.exit(1))(run)
  }

  case class Levels(r: (Int, Int), g: (Int, Int), b: (Int, Int), value: (Int, Int))

  def applyLevels(in: GE, levels: Levels): GE = {
    import levels._
    val chans = r :: g :: b :: Nil
    val rgb   = Seq(in \ 0, in \ 1, in \ 2): GE
    val alpha = in \ 3
    val low   = chans.map(tup => tup._1 / 255.0: GE)
    val high  = chans.map(tup => tup._2 / 255.0: GE)
    val p1    = rgb.linlin(low, high, 0, 1)
    val p2    = p1 .linlin(value._1 / 255.0, value._2 / 255.0, 0, 1)
    val clip  = p2 .max(0).min(1).elastic(10)
    Seq(clip \ 0, clip \ 1, clip \ 2, alpha): GE
  }

//  def invert(in: GE): GE = {
//    val rgb   = Seq(in \ 1, in \ 2, in \ 3): GE
//    val alpha = in \ 0
//    val inv   = (new GEOps2(-rgb) + 1).elastic(2)
//    Seq(alpha, inv \ 0, inv \ 1, inv \ 2): GE
//  }

  def invert(in: GE): GE = {
    val rgb   = Seq(in \ 0, in \ 1, in \ 2): GE
    val alpha = in \ 3
    val inv   = (-rgb + (1: GE)).elastic(2)
    Seq(inv \ 0, inv \ 1, inv \ 2, alpha): GE
  }

  def resample(in: GE, widthIn: GE, heightIn: GE, widthOut: GE, heightOut:GE): GE = {
    import graph._
    val scaleX  = widthOut  / widthIn
    val scaleY  = heightOut / heightIn
    val rx      = Resample      (in                 , factor = scaleX, minFactor = scaleX)
    val ry      = ResampleWindow(rx, size = widthOut, factor = scaleY, minFactor = scaleY)
    ry.max(0).min(1)
  }

  def position(in: GE, widthIn: GE, heightIn: GE, widthOut: GE, heightOut: GE, x: GE, y: GE): GE = {
    import graph._
    val wo = widthOut
    val ho = heightOut
    val px = ResizeWindow(in, size = widthIn      , start = -x     , stop =  wo - (widthIn  + x))
    val py = ResizeWindow(px, size = wo * heightIn, start = -y * wo, stop = (ho - (heightIn + y)) * wo)
    py
  }

  def run(config: Config): Unit = {
    import config._
    val g = Graph {
      import graph._
      val fIn0  = formatFile(inTemp , 1)
      val fOut0 = formatFile(outTemp, 1)
      val in    = ImageFileIn(fIn0, numChannels = 4)

      val widthIn   = 773
      val heightIn  = 1059
      val widthOut  = 1024
      val heightOut = 1024
      val widthR    = widthIn  / 2
      val heightR   = heightIn /  2
      val inR       = resample(in, widthIn, heightIn, widthR, heightR)

      val lvl       = Levels(r = 41 -> 254, g = 41 -> 254, b = 41 -> 254, value = 0 -> 200)

//      val sig   = SinOsc((Seq[GE](0.5, 1.0, 1.5, 2.0): GE) / 766).linlin(-1, 1, 0, 1)
      val inLvl   = invert(applyLevels(inR, lvl))

//      val black   = DC(Seq[GE](0.0, 0.0, 0.0)).take(widthOut * heightOut)

      val pos     = position(inLvl, widthIn = widthR, heightIn = heightR, widthOut = widthOut, heightOut = heightOut, x = 10, y = 10)

      val sig     = Seq(pos \ 0, pos \ 1, pos \ 2): GE

      val spec  = ImageFile.Spec(width = widthOut, height = heightOut, numChannels = 3 /* 1 */,
        fileType = ImageFile.Type.PNG, sampleFormat = ImageFile.SampleFormat.Int8,
        quality = 100)
      ImageFileOut(fOut0, spec, in = sig)
    }
    val cc  = Control.Config()
    val ctl = Control(cc)
    ctl.run(g)

    import scala.concurrent.ExecutionContext.Implicits.global
    ctl.status.onComplete {
      x => println(s"Result: $x")
    }
  }
}