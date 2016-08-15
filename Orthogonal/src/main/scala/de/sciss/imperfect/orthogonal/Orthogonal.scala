/*
 *  Orthogonal.scala
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

package de.sciss.imperfect.orthogonal

import de.sciss.file._
import de.sciss.fscape._
import de.sciss.fscape.gui.SimpleGUI
import scopt.OptionParser

import scala.swing.Swing

object Orthogonal {
  case class Config(
                    // templateIn  : File    = userHome / "Documents" / "projects" / "Unlike" / "moor_8024" / "moor_8024-%05d.jpg",
                    templateIn  : File    = userHome / "Documents" / "projects" / "Unlike" / "moor_8024_out" / "moor_8024-out-%05d.jpg",
                    templateOut : File    = userHome / "Documents" / "projects" / "Imperfect" / "moor_ortho" / "frame-%d.jpg",
                    strange     : Boolean = false,
                    idxRange0   : Range   = 1 to 11541, // 11945,
                    width       : Int     = 1024,
                    height      : Int     = 1024,
                    trimLeft    : Int     = 400,
                    trimTop     : Int     = 24,
                    gain        : Double  = 32.0,
                    clip        : Double  = math.Pi / 8,
                    smear       : Double  = 0.97,
                    noise       : Double  = 1.0
                   )

  def main(args: Array[String]): Unit = {
    val parser = new OptionParser[Config]("Orthogonal") {
      opt[File  ]('i', "template-in")  text "Image file template input"  action { (x, c) => c.copy(templateIn  = x) }
      opt[File  ]('o', "template-out") text "Image file template output" action { (x, c) => c.copy(templateOut = x) }
      opt[Unit  ]("strange")           text "Strange scan mode" action { (_, c) => c.copy(strange = true) }
      opt[Int   ]("start")             text "Start frame (inclusive)" action { (x, c) =>
        c.copy(idxRange0 = if (c.idxRange0.isInclusive) x to c.idxRange0.end else x until c.idxRange0.end)
      }
      opt[Int   ]("stop")              text "End frame (exclusive)" action { (x, c) =>
        c.copy(idxRange0 = c.idxRange0.start until x)
      }
      opt[Int   ]("last")              text "End frame (inclusive)" action { (x, c) =>
        c.copy(idxRange0 = c.idxRange0.start to x)
      }
      opt[Int   ]('w', "width")        text "Image width in pixels"  action { (x, c) => c.copy(width  = x) }
      opt[Int   ]('h', "height")       text "Image height in pixels" action { (x, c) => c.copy(height = x) }
      opt[Int   ]('l', "trim-left")    action { (x, c) => c.copy(trimLeft = x) }
      opt[Int   ]('t', "trim-top")     action { (x, c) => c.copy(trimTop  = x) }
      opt[Double]('g', "gain")         action { (x, c) => c.copy(gain = x) }
      opt[Double]('c', "clip")         action { (x, c) => c.copy(clip = x) }
      opt[Double]("smear")             action { (x, c) => c.copy(smear = x) }
      opt[Double]("noise")             action { (x, c) => c.copy(noise = x) }
    }
    parser.parse(args, Config()).fold(sys.exit(1))(run)
  }

  def run(config: Config): Unit = {
    import config._

    val idxRange      = idxRange0.map(x => x: GE)
    val numInput      = idxRange.size
    val indices       = idxRange.reduce(_ ++ _)   // XXX TODO --- we need a better abstraction for this
    val widthIn       = 1920  // XXX TODO read from first input image!
    val heightIn      = 1080  // XXX TODO read from first input image!
    val trimRight     = widthIn  - width  - trimLeft
    val trimBottom    = heightIn - height - trimTop
    val frameSizeIn   = widthIn * heightIn
    val frameSize     = width * height
//    val medianLen     = medianSide * 2 + 1

    require(trimLeft >= 0 && trimRight >= 0 && trimTop >= 0 && trimBottom >= 0)

    val streamCfg = stream.Control.Config()
    streamCfg.blockSize  = width * 2
    streamCfg.useAsync   = false

    val g = Graph {
      import graph._

      def trim(in: GE): GE = {
        val half1 = ResizeWindow(in   , size = widthIn         , start = trimLeft       , stop = -trimRight)
        // strange-artifact-1: use `size = frameSizeIn`, `trimLeft = 400`, `trimTop = 400`.
        val half2 = ResizeWindow(half1, size = if (strange) frameSizeIn else width * heightIn,
          start = width * trimTop, stop = -width * trimBottom)
        half2
      }

//      def normalize(in: GE, headroom: GE = 1): GE = {
//        val max       = RunningMax(in.abs).last
//        val gain      = max.reciprocal * headroom
//        gain.ampdb.roundTo(0.01).poll(0, "gain [dB]")
//        // Plot1D(in, width * height)
//        // in.poll(1.0/32, label = "test")
//        val buf       = BufferDisk(in)
//        buf * gain
//      }

      def normalizeFrames(in: GE): GE = {
        val tr        = Impulse(1.0/frameSize)
        val trG       = Impulse(1.0/frameSize)
        val min       = RunningMin(in, tr)
        val max       = RunningMax(in, tr)
        val minD      = min.drop(frameSize - 1)
        val maxD      = max.drop(frameSize - 1)
        val minG      = Gate(minD, trG)
        val maxG      = Gate(maxD, trG)
//        val step      = (0: GE) ++ (0.98 /* .pow(1.0/frameSize) */ : GE)
//        val minLag    = OnePoleWindow(minG, size = frameSize, coef = step)
//        val maxLag    = OnePoleWindow(maxG, size = frameSize, coef = step)
        val step      = (0: GE) ++ (0.999.pow(1.0/frameSize): GE)

//        ChannelProxy(minG, 0).poll(1.0/frameSize, "min-red  ")
//        ChannelProxy(minG, 1).poll(1.0/frameSize, "min-green")
//        ChannelProxy(minG, 2).poll(1.0/frameSize, "min-blue ")
//
//        ChannelProxy(maxG, 0).poll(1.0/frameSize, "max-red  ")
//        ChannelProxy(maxG, 1).poll(1.0/frameSize, "max-green")
//        ChannelProxy(maxG, 2).poll(1.0/frameSize, "max-blue ")

//        val minLag    = OnePole(minG, coef = step)
//        val maxLag    = OnePole(maxG, coef = step)
        val minLag    = OnePole(minG.max(-1).min(1), coef = step)
        val maxLag    = OnePole(maxG.max(-1).min(1), coef = step)

        //        val buf       = BufferDisk(in)
        val buf       = in.elastic(frameSize/streamCfg.blockSize + 1)
//        gain.ampdb.roundTo(0.01).poll(0, "gain [dB]")
        // Plot1D(in, width * height)
        // in.poll(1.0/32, label = "test")
//        val buf       = BufferDisk(in)
        buf.linlin(minLag, maxLag, 0.0, 1.0)
      }

      def mkImgSeq() = {
        val res = ImageFileSeqIn(template = templateIn, numChannels = 3, indices = indices)
        trim(res)
      }

      val bufIn = mkImgSeq()
      val ortho = GramSchmidtMatrix(in = bufIn, rows = width, columns = height, normalize = 0)
      val spec  = ImageFile.Spec(width = width, height = height, numChannels = /* 1 */ 3,
        fileType = ImageFile.Type.JPG /* PNG */, sampleFormat = ImageFile.SampleFormat.Int8,
        quality = 100)

      // make first line black for now
      val ortho1  = ResizeWindow(in = ortho , size = frameSize        , start =  width, stop = 0)
      val ortho2  = ResizeWindow(in = ortho1, size = frameSize - width, start = -width, stop = 0)
      val eq      = (ortho2 * gain).tan
      val sm      = OnePoleWindow(in = eq, size = frameSize, coef = smear)
//      val sig0    = normalizeFrames(smear)
      val sig0    = sm.linlin(-clip, clip, 0, 1)
      val sig1    = if (noise <= 0) sig0 else sig0 + WhiteNoise(noise/255).take(frameSize.toLong * numInput)
      val sig     = sig1.max(0).min(1)
//      val sig = ortho.linlin(-1, 1, 0, gain).max(0).min(1)

      val idxRangeOut = 1 to idxRange.size // - (medianLen - 1)
      val indicesOut = idxRangeOut.map(x => x: GE).reduce(_ ++ _)
      ImageFileSeqOut(template = templateOut, spec = spec, in = sig, indices = indicesOut)
    }

    val ctrl = stream.Control(streamCfg)
    ctrl.run(g)

    Swing.onEDT {
      SimpleGUI(ctrl)
    }

    println("Running.")
  }
}