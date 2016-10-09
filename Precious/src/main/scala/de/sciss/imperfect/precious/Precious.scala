/*
 *  Precious.scala
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

package de.sciss.imperfect.precious

import java.awt.geom.AffineTransform

import de.sciss.file._
import de.sciss.fscape.gui.SimpleGUI
import de.sciss.fscape.stream.Control
import de.sciss.fscape.{GE, Graph, graph}

import scala.swing.Swing

object Precious {
  case class Config(index: Int, cx: Int, cy: Int, diam: Int,
                    levelsLo: Int = 0, levelsHi: Int = 255, gamma: Double = 1.0,
                    startAngle: Double, angleSpan: Double, numFrames: Int)

  val cfgCup1 = Config(index = 9948, cx = 1972, cy = 2632, diam = 2400,
    levelsLo = 19, levelsHi = 233, gamma = 1.0,
    startAngle = 0.0, angleSpan = 2.222 * 360, numFrames = 24 * 60 * 2)
  val cfgCup2 = Config(index = 9947, cx = 1972 - 5, cy = 2632 + 10, diam = 2400,
    levelsLo = 19, levelsHi = 233, gamma = 0.8,
    startAngle = 0.0, angleSpan = 2.222 * 360, numFrames = 24 * 60 * 2)

  def main(args: Array[String]): Unit = {
    run(cfgCup2)
  }

  def applyLevels(in: GE, lo: Int, hi: Int, hasAlpha: Boolean = false): GE = if (lo == 0 && hi == 255) in else {
    val rgb   = if (hasAlpha) Seq.tabulate(3)(ch => in \ ch): GE else in
    val low   = lo / 255.0
    val high  = hi / 255.0
    val p2    = rgb.linlin(low, high, 0, 1)
    val flt   = p2 .max(0).min(1) // .elastic(10)
    val out   = if (hasAlpha) {
      val clipE = flt.elastic(5)
      Seq.tabulate(4)(ch => (if (ch == 3) in else clipE) \ ch): GE
    } else flt
    out
  }

  def applyGamma(in: GE, gamma: Double, hasAlpha: Boolean = false): GE = if (gamma == 1) in else {
    val rgb   = if (hasAlpha) Seq.tabulate(3)(ch => in \ ch): GE else in
    val flt   = rgb.pow(1.0 / gamma)
    val out   = if (hasAlpha) {
      val clipE = flt.elastic(1)
      Seq.tabulate(4)(ch => (if (ch == 3) in else clipE) \ ch): GE
    } else flt
    out
  }

  def run(config: Config): Unit = {
    val streamConfig = Control.Config()
    streamConfig.useAsync = false
    //  config.blockSize = 3456
    var gui: SimpleGUI = null
    streamConfig.progressReporter = rep => Swing.onEDT(gui.progress = rep.total)
    val streamControl = Control(streamConfig)

    val tempIn    = userHome / "Documents" / "projects" / "Imperfect" / "photos" / "161008_HH"/ "_MG_%d.JPG"
    val dirOut    = userHome / "Documents" / "projects" / "Imperfect" / "precious" / config.index.toString
    dirOut.mkdirs()
    val tempOut   = dirOut / "frame-%d.png"

    val g = Graph {
      import graph._

      val widthIn0  = 3456
      val heightIn0 = 5184

      val widthIn   = heightIn0
      val heightIn  = widthIn0
      val widthOut  = 1024
      val heightOut = 1024
      val frameSizeOut  = widthOut.toLong * heightOut

      val cx0       = config.cx // 1972 // 1947
      val cy0       = config.cy // 2632 // 2700
      val cxIn      = -cy0
      val cyIn      = widthIn0 - cx0

      val cxOut     = widthOut  * 0.5
      val cyOut     = heightOut * 0.5

      // val radiusMax = Seq(cy, heightIn - cy, cx, widthIn - cx).min
      val diam      = config.diam // 2400 // 2500 //   2750
      val scale     = widthOut.toDouble / diam

//      val rotations = 0 until 360 by 10
      val rotations = (0 until config.numFrames).map { frame =>
        val dAng    = config.angleSpan * frame / config.numFrames
        val newAng  = config.startAngle + dAng
//        println(f"ang: $newAng%1.2f")
        dAng
      }
      val seqLen    = rotations.size

      val indexIn   = config.index // 9948
      val indicesIn = Seq.fill(seqLen)(indexIn: GE).reduce(_ ++ _)
      val img0      = ImageFileSeqIn(template = tempIn, numChannels = 3, indices = indicesIn)
      val img1      = applyLevels(img0, lo = config.levelsLo, hi = config.levelsHi)
      val img       = applyGamma(img1, gamma = config.gamma)

      val transforms = rotations.map { rotDeg =>
        val rot = math.Pi / 180 * rotDeg
        val at = new AffineTransform
        at.preConcatenate(AffineTransform.getRotateInstance(rot, cxIn, cyIn))
        at.preConcatenate(AffineTransform.getTranslateInstance(-cxIn, -cyIn))
        at.preConcatenate(AffineTransform.getScaleInstance(scale, scale))
        at.preConcatenate(AffineTransform.getTranslateInstance(cxOut, cyOut))
        val _mat = new Array[Double](6)
        at.getMatrix(_mat)
        _mat
      }
      val transformsT = transforms.transpose
      // println(s"SHAPE: [${transformsT.size}][${transformsT(0).size}]")

      val mat = transformsT.zipWithIndex.map { case (cSeq, cIdx) =>
        val cSeqGE = cSeq.map(x => x: GE).reduce(_ ++ _)
        RepeatWindow(cSeqGE, num = frameSizeOut)
      }

      val sig0      = AffineTransform2D(img, widthIn = widthIn, heightIn = heightIn, widthOut = widthOut, heightOut = heightOut,
        m00 = mat(0), m10 = mat(1), m01 = mat(2), m11 = mat(3), m02 = mat(4), m12 = mat(5),
        zeroCrossings = 4 /* 7 */, wrap = 1)
      val sig       = sig0.max(0).min(1).elastic(frameSizeOut / streamControl.blockSize)

      val spec  = ImageFile.Spec(width = widthOut, height = heightOut, numChannels = 3, fileType = ImageFile.Type.PNG)
      //    ImageFileOut(file = fOut, spec = spec, in = sig)
      val indicesOut = (1 to seqLen).map(x => x: GE).reduce(_ ++ _)
      ImageFileSeqOut(template = tempOut, spec = spec, in = sig, indices = indicesOut)

      Progress(in = Frames(ChannelProxy(sig, 0)) / (frameSizeOut * seqLen), Metro(widthOut /* frameSizeOut */))
    }

    Swing.onEDT {
      gui = SimpleGUI(streamControl)
    }

    streamControl.run(g)

    // println("Running.")
  }
}
