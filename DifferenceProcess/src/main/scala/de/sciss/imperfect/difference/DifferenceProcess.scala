/*
 *  DifferenceProcess.scala
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

package de.sciss.imperfect.difference

import de.sciss.file._
import de.sciss.fscape.gui.SimpleGUI
import de.sciss.fscape._
import de.sciss.synth.io.AudioFileSpec
import scopt.OptionParser

import scala.swing.Swing

object DifferenceProcess {
  case class Config(templateIn  : File    = file("/") /"media" / "hhrutz" / "PRINT_DESK" / "site-9" / "frame-%d.jpg",
                    templateOut : File    = userHome / "Documents" / "projects" / "Imperfect" / "site-9" / "frame-%d.jpg",
                    strange     : Boolean = false,
                    idxRange0   : Range   = 1 to 500,
                    width       : Int     = 1024,
                    height      : Int     = 1024,
                    trimLeft    : Int     = 800,
                    trimTop     : Int     = 500,
                    gain        : Double  = 16.0,
                    gamma       : Double  = 1.2,
                    seqLen      : Int     = 30,
                    medianSide  : Int     = 3,
                    thresh      : Double  = 0.1
                   )

//  val baseDirOut    = userHome / "Documents" / "projects" / "Imperfect" / (if (STRANGE) "site-9s" else "site-9")
//  val templateOut   = baseDirOut / "frame-%d.jpg")

  def main(args: Array[String]): Unit = {
    val parser = new OptionParser[Config]("DifferenceProcess") {
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
      opt[Double]("gain")              text "Gain factor"  action { (x, c) => c.copy(gain = x) }
      opt[Double]("threshold")         action { (x, c) => c.copy(thresh = x) }
      opt[Int   ]('n', "seq-len")      text "Sliding window length" action { (x, c) => c.copy(seqLen = x) }
      opt[Int   ]('m', "median-side")  text "Median side length" action { (x, c) => c.copy(medianSide = x) }
    }
    parser.parse(args, Config()).fold(sys.exit(1))(run)
  }

  def run(config: Config): Unit = {
    import config._

    val SEQUENCE      = true
    val idxRange      = (if (SEQUENCE) idxRange0 else idxRange0.take(30)).map(x => x: GE)
    val numInput      = idxRange.size
    val indices       = idxRange.reduce(_ ++ _)   // XXX TODO --- we need a better abstraction for this
      val widthIn       = 3280  // XXX TODO read from first input image!
    val heightIn      = 2464  // XXX TODO read from first input image!
//    val width         = 1920
//    val height        = 1080
//    val width         = 1024
////    val height        = 1024
//    val trimLeft      = if (strange) 400 else 840
//    val trimTop       = if (strange) 400 else 720
//    val gain          = 4.5 // 9.0
//    val gamma         = if (strange) 1.4 else 1.2 // 1.4
//    val seqLen        = 30
    val trimRight     = widthIn  - width  - trimLeft
    val trimBottom    = heightIn - height - trimTop
    val frameSizeIn   = widthIn * heightIn
    val frameSize     = width * height
//    val thresh        = if (strange) 0.05 else 0.2 // 0.01 // 0.05 // 0.0 // 0.3333
//    val medianSide     = 3 // 2
    val medianLen     = medianSide * 2 + 1

    require(trimLeft >= 0 && trimRight >= 0 && trimTop >= 0 && trimBottom >= 0)
    //    val thresh    = 0.2 / 150

    //    val fOut      = userHome / "Documents" / "temp" / "out_median.png"
    val fOut      = userHome / "Documents" / "temp" / "out_median.jpg"

    //    val fltBlur = new SmartBlurFilter
    //    fltBlur.setRadius(7)
    //    fltBlur.setThreshold(20)

    val streamCfg = stream.Control.Config()
    streamCfg.blockSize  = width * 2
    streamCfg.useAsync   = false

    val g = Graph {
      import graph._

      def blur(in: GE): GE = in // XXX TODO --- perhaps 2D-FFT-based convolution --- fltBlur.filter(in, null)

      // actually "negative delay"
      def delayFrame(in: GE, n: Int = 1): GE = in.drop(frameSize * n)

      def extractBrightness(in: GE): GE = {
        val r   = ChannelProxy(in, 0)
        val g   = ChannelProxy(in, 1)
        val b   = ChannelProxy(in, 2)
        (0.299 * r.squared + 0.587 * g.squared + 0.114 * b.squared).sqrt
      }

      def quarter(in: GE): GE = {
        val half1 = ResizeWindow(in   , size = widthIn         , start = trimLeft       , stop = -trimRight)
        // strange-artifact-1: use `size = frameSizeIn`, `trimLeft = 400`, `trimTop = 400`.
        val half2 = ResizeWindow(half1, size = if (strange) frameSizeIn else width * heightIn,
          start = width * trimTop, stop = -width * trimBottom)
        half2
      }

      def normalize(in: GE, headroom: GE = 1): GE = {
        val max       = RunningMax(in.abs).last
        val gain      = max.reciprocal * headroom
        gain.ampdb.roundTo(0.01).poll(0, "gain [dB]")
        // Plot1D(in, width * height)
        // in.poll(1.0/32, label = "test")
        val buf       = BufferDisk(in)
        buf * gain
      }

      def mkImgSeq() = {
        val res = ImageFileSeqIn(template = templateIn, numChannels = 3, indices = indices)
        quarter(res)
      }

      val bufIn     = mkImgSeq()
      //      val bufIn     = WhiteNoise(Seq.fill[GE](3)(0.5)).take(frameSize * idxRange.size)
      val blurImg   = blur(bufIn)
      val lum       = extractBrightness(blurImg)

      //      Length(bufIn).poll(0, "bufIn.length")
      //      Length(lum  ).poll(0, "lum  .length")
      //      RunningSum(bufIn).poll(1.0/frameSize, "PING")

      //      // XXX TODO --- or Sliding(lum, frameSize * medianLen, frameSize) ?
      //      val lumWin    = (Vector(lum) /: (0 until medianLen)) { case (res @ (init :+ last), _) =>
      //        res :+ delayFrame(last)
      //      }
      //
      //      val lumC      = lumWin(sideLen)

      val lumSlide  = Sliding(lum, size = frameSize * medianLen, step = frameSize)
      //      val lumT      = TransposeMatrix(lumSlide, columns = frameSize, rows = medianLen)
      //      val comp0     = delayFrame(lum, n = sideLen)
      ////      val comp      = comp0.elastic((sideLen * frameSize + config.blockSize - 1) / config.blockSize)
      //      val comp      = BufferDisk(comp0)
      val dly   = delayFrame(mkImgSeq(), n = medianSide).take(frameSize * (numInput - (medianLen - 1))) // .dropRight(sideLen * frameSize)
      val comp  = extractBrightness(blur(dly))

      //      val runTrig   = Impulse(1.0 / medianLen)
      //      val minR      = RunningMin(lumT, runTrig)
      //      val maxR      = RunningMax(lumT, runTrig)
      //      val meanR     = RunningSum(lumT, runTrig) / medianLen
      //      val min       = Sliding(minR .drop(medianLen - 1), size = 1, step = medianLen)
      //      val max       = Sliding(maxR .drop(medianLen - 1), size = 1, step = medianLen)
      //      // XXX TODO --- use median instead of mean
      //      val mean      = Sliding(meanR.drop(medianLen - 1), size = 1, step = medianLen)

      val medianTrig = Impulse(1.0/(frameSize * medianLen))
      val minR      = RunningWindowMin(lumSlide, size = frameSize, trig = medianTrig)
      val maxR      = RunningWindowMax(lumSlide, size = frameSize, trig = medianTrig)
      val sumR      = RunningWindowSum(lumSlide, size = frameSize, trig = medianTrig)
      //      val min       = ResizeWindow(minR, size = medianLen, start = medianLen - 1, stop = 0)
      val min       = ResizeWindow(minR, size = frameSize * medianLen, start = frameSize * (medianLen - 1), stop = 0)
      val max       = ResizeWindow(maxR, size = frameSize * medianLen, start = frameSize * (medianLen - 1), stop = 0)
      val mean      = ResizeWindow(sumR, size = frameSize * medianLen, start = frameSize * (medianLen - 1), stop = 0) / medianLen

      //      Length(min  ).poll(0, "min  .length")

      val maskIf    = (max - min > thresh) * ((comp sig_== min) max (comp sig_== max))
      val mask      = maskIf * {
        val med = mean // medianArr(sideLen)
        comp absdif med
      }
      val maskBlur  = blur(mask)

      // XXX TODO
      // mix to composite
      // - collect 'max' pixels for comp * maskBlur

      //      lumSlide.poll(1.0/frameSize, "lumSlide")
      //      lumT    .poll(1.0/frameSize, "lumT    ")
      //      min     .poll(1.0/frameSize, "min     ")
      //      max     .poll(1.0/frameSize, "max     ")
      maskBlur.poll(1.0/frameSize, "maskBlur")

      val sel     = maskBlur * dly
      //      val expose  = RunningWindowMax(sel, size = frameSize)
      val expose  = RunningWindowSum(sel, size = frameSize)

      //      val test  = min /* maskBlur */ .take(frameSize * (numInput - (medianLen - 1))).takeRight(frameSize)

      if (SEQUENCE) {
        //        val selDly      = delayFrame(sel, n = seqLen)
        //        val exposeDly   = RunningWindowSum(selDly, size = frameSize)
        val exposeDly = delayFrame(expose, n = seqLen)
        //        val dlyElastic  = (seqLen * frameSize) / config.blockSize + 1
        //        val exposeSlid  = expose.elastic(dlyElastic) - exposeDly
        // OutOfMemoryError -- buffer to disk instead
        val exposeSlid  = exposeDly - BufferDisk(expose)
        val sig: GE = if (strange) {
          val in        = exposeSlid
          val resetTr   = Impulse(1.0 / frameSize)
          val maxR      = RunningMax(in.abs, trig = resetTr)
          val max       = Gate(maxR.drop(frameSize - 1), Impulse(1.0 / frameSize))
          val gain      = max.reciprocal // * headroom
          val buf       = BufferDisk(in)
          buf * gain
        } else {
          (exposeSlid * gain).max(0.0).min(1.0).pow(gamma)
        }

        val spec  = ImageFile.Spec(width = width, height = height, numChannels = /* 1 */ 3,
          fileType = ImageFile.Type.JPG /* PNG */, sampleFormat = ImageFile.SampleFormat.Int8,
          quality = 100)
//        val indicesOut = indices.drop(sideLen).take(idxRange.size - (medianLen - 1))
        val idxRangeOut = 1 to idxRange.size - (medianLen - 1)
        val indicesOut = idxRangeOut.map(x => x: GE).reduce(_ ++ _)
          ImageFileSeqOut(template = templateOut, spec = spec, in = sig, indices = indicesOut)

      } else {
        val last  = expose.take(frameSize * (numInput - (medianLen - 1))).takeRight(frameSize)
        // min.take(frameSize * (numInput - (medianLen - 1))).takeRight(frameSize)
        val sig   = normalize(last)
        val spec  = ImageFile.Spec(width = width, height = height, numChannels = /* 1 */ 3,
          fileType = ImageFile.Type.JPG /* PNG */, sampleFormat = ImageFile.SampleFormat.Int8,
          quality = 100)
        ImageFileOut(file = fOut, spec = spec, in = sig)
        // "full resolution copy"
        AudioFileOut(file = fOut.replaceExt("aif"),
          spec = AudioFileSpec(numChannels = 3, sampleRate = 44100), in = sig)
      }
    }

    val ctrl = stream.Control(streamCfg)
    ctrl.run(g)

    Swing.onEDT {
      SimpleGUI(ctrl)
    }

    println("Running.")
  }

//  def convertTest(): Unit = {
//    levelsBin(in = baseDir / "out_median.bin", out = baseDir / "out_medianColr.png",
//      width = 3280, height = 2464, overLo = 3, overHi = 99.9, gamma = 1.0 /* 1.125 */)
//  }

  // compares strings insensitive to case but sensitive to integer numbers
  def compareName(s1: String, s2: String): Int = {
    // this is a quite ugly direct translation from a Java snippet I wrote,
    // could use some scala'fication

    val n1  = s1.length
    val n2  = s2.length
    val min = math.min(n1, n2)

    var i = 0
    while (i < min) {
      var c1 = s1.charAt(i)
      var c2 = s2.charAt(i)
      var d1 = Character.isDigit(c1)
      var d2 = Character.isDigit(c2)

      if (d1 && d2) {
        // Enter numerical comparison
        var c3 = c1
        var c4 = c2
        var sameChars = c3 == c4
        do {
          i += 1
          val c5 = if (i < n1) s1.charAt(i) else 'x'
          val c6 = if (i < n2) s2.charAt(i) else 'x'
          d1 = Character.isDigit(c5)
          d2 = Character.isDigit(c6)
          if (sameChars && c5 != c6) {
            c3 = c5
            c4 = c6
            sameChars = false
          }
        }
        while (d1 && d2)

        if (d1 != d2) return if (d1) 1 else -1  // length wins
        if (!sameChars) return c3 - c4          // first diverging digit wins
        i -= 1
      }
      else if (c1 != c2) {
        c1 = Character.toUpperCase(c1)
        c2 = Character.toUpperCase(c2)

        if (c1 != c2) {
          c1 = Character.toLowerCase(c1)
          c2 = Character.toLowerCase(c2)

          if (c1 != c2) {
            // No overflow because of numeric promotion
            return c1 - c2
          }
        }
      }

      i += 1
    }
    n1 - n2
  }

//  def percentile[A](xs: IndexedSeq[A], n: Double): A = xs(((xs.size - 1) * n / 100.0 + 0.5).toInt)
//
//  def levelsBin(in: File, out: File, width: Int, height: Int, overLo: Double = 2, overHi: Double = 98, gamma: Double = 1.5): Unit = {
//    val gammaInv = 1.0 / gamma
//    composite = readBinary(in)
//    composite.foreach { ch =>
//      val sorted = (ch: IndexedSeq[Double]).sortedT
//      val low    = percentile(sorted, overLo)
//      val high   = percentile(sorted, overHi)
//      var i = 0
//      while (i < ch.length) {
//        ch(i) = ch(i).clip(low, high).linlin(low, high, 0, 1).pow(gammaInv)
//        i += 1
//      }
//    }
//
//    val bufOut = mkBlackImage(width, height)
//    composite.zipWithIndex.foreach { case (plane, ch) =>
//      fillChannel(plane.grouped(width).toArray, bufOut, chan = ch)
//    }
//    ImageIO.write(bufOut, "png", out)
//  }
}