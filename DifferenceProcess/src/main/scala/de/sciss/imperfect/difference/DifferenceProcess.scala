/*
 *  DifferenceProcess.scala
 *  (Imperfect Reconstruction)
 *
 *  Copyright (c) 2016-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v2+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.imperfect.difference

import de.sciss.file._
import de.sciss.fscape._
import de.sciss.fscape.graph.ArithmSeq
import de.sciss.fscape.gui.SimpleGUI
import de.sciss.synth.io.AudioFileSpec
import scopt.OptionParser

import scala.swing.Swing

object DifferenceProcess {
  case class Config(templateIn  : File    = file("/") /"media" / "hhrutz" / "PRINT_DESK" / "in" / "frame-%d.jpg",
                    templateOut : File    = userHome / "Documents" / "projects" / "Imperfect" / "out" / "frame-%d.jpg",
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
                    thresh      : Double  = 0.1,
                    redGain     : Double  = 1.0,
                    greenGain   : Double  = 1.0,
                    blueGain    : Double  = 1.0,
                    frameStep   : Int     = 1,
                    rotate      : Int     = 0,
                    autoLevels  : Boolean = false
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
      opt[Int   ]("frame-step")
          .text ("Step in output frames (default: 1)")
          .validate { i => if (i > 0) success else failure(s"frame-step must be > 0") }
          .action { (x, c) => c.copy(frameStep = x) }
      opt[Double]("red-gain")          action { (x, c) => c.copy(redGain   = x) }
      opt[Double]("green-gain")        action { (x, c) => c.copy(greenGain = x) }
      opt[Double]("blue-gain")         action { (x, c) => c.copy(blueGain  = x) }
      opt[Int   ]("rotate")
          .text("Rotate output; allowed values: 0 (none), 90 (clock-wise), -90 (anti-clock-wise), 180")
          .validate(i => if (-90 to 180 by 90 contains i) success else failure("Only 0, 90, -90, 180 allowed"))
          .action { (x, c) => c.copy(rotate    = x) }
      opt[Unit  ]("auto-levels") action { (_, c) => c.copy(autoLevels = true) }
    }
    parser.parse(args, Config()).fold(sys.exit(1))(run)
  }

  def run(config: Config): Unit = {
    import config._

    if ((height & 1) != 0) {
      println("Warning: DifferenceProcess has a bug when height is not an even number!")
    }

    val SEQUENCE      = true
    val idxRange      = (if (SEQUENCE) idxRange0 else idxRange0.take(30)).map(x => x: GE)
    val numInput      = idxRange.size
//    val indices       = idxRange.reduce(_ ++ _)
    val indices       = ArithmSeq(start = idxRange.head, step = 1, length = idxRange.size)
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
    val frameSizeL    = frameSize.toLong
//    val thresh        = if (strange) 0.05 else 0.2 // 0.01 // 0.05 // 0.0 // 0.3333
//    val medianSide     = 3 // 2
    val medianLen     = medianSide * 2 + 1

    require(trimLeft   >= 0, s"trimLeft ($trimLeft) must be >= 0")
    require(trimRight  >= 0, s"trimRight ($trimRight) must be >= 0")
    require(trimTop    >= 0, s"trimTop ($trimTop) must be >= 0")
    require(trimBottom >= 0, s"trimBottom ($trimBottom) must be >= 0")
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
      def delayFrame(in: GE, n: Int): GE = in.drop(frameSizeL * n)

      def extractBrightness(in: GE): GE = {
        val r   = ChannelProxy(in, 0)
        val g   = ChannelProxy(in, 1)
        val b   = ChannelProxy(in, 2)
        (r.squared * 0.299 + g.squared * 0.587 + b.squared * 0.114).sqrt
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

      val bufIn = mkImgSeq()
//      val bufIn: GE = if (redGain == 1.0 && blueGain == 1.0 && greenGain == 1.0) bufIn0 else {
//        println(s"redGain = $redGain, greenGain = $greenGain, blueGain = $blueGain")
//        val r = ChannelProxy(bufIn0, 0) * 1.01 // redGain
//        val g = ChannelProxy(bufIn0, 1) * 1.01 // greenGain
//        val b = ChannelProxy(bufIn0, 2) * 1.01 // blueGain
//        Seq(r, g, b)
//      }

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

      val lumSlide  = Sliding(lum, size = frameSizeL * medianLen, step = frameSize)
      //      val lumT      = TransposeMatrix(lumSlide, columns = frameSize, rows = medianLen)
      //      val comp0     = delayFrame(lum, n = sideLen)
      ////      val comp      = comp0.elastic((sideLen * frameSize + config.blockSize - 1) / config.blockSize)
      //      val comp      = BufferDisk(comp0)
      val dly   = delayFrame(mkImgSeq(), n = medianSide).take(frameSizeL * (numInput - (medianLen - 1))) // .dropRight(sideLen * frameSize)
      val comp  = extractBrightness(blur(dly))

      //      val runTrig   = Metro(medianLenL)
      //      val minR      = RunningMin(lumT, runTrig)
      //      val maxR      = RunningMax(lumT, runTrig)
      //      val meanR     = RunningSum(lumT, runTrig) / medianLen
      //      val min       = Sliding(minR .drop(medianLen - 1), size = 1, step = medianLen)
      //      val max       = Sliding(maxR .drop(medianLen - 1), size = 1, step = medianLen)
      //      // XXX TODO --- use median instead of mean
      //      val mean      = Sliding(meanR.drop(medianLen - 1), size = 1, step = medianLen)

      val medianTrig = Metro(frameSizeL * medianLen)
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
      // maskBlur.poll(frameSize, "maskBlur")

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
          val resetTr   = Metro(frameSize)
          val maxR      = RunningMax(in.abs, trig = resetTr)
          val max       = Gate(maxR.drop(frameSize - 1), Metro(frameSize))
          val gain      = max.reciprocal // * headroom
          val buf       = BufferDisk(in)
          buf * gain
        } else {
          val gain1 = gain * (Seq[GE](redGain, greenGain, blueGain): GE)
          (exposeSlid * gain1).max(0.0).min(1.0).pow(gamma)
        }

        val tpe         = if (templateOut.ext == "png") ImageFile.Type.PNG else ImageFile.Type.JPG
        val stopFrame   = (idxRange.size - (medianLen - 1)) / frameStep
        val idxRangeOut = 1 to stopFrame
        val sigOut0     = if (frameStep == 1) sig else {
          ResizeWindow(sig, size = frameSizeL * frameStep, start = 0, stop = -(frameSizeL * (frameStep - 1)))
        }
        val sigOut1 = rotate match {
          case   0 => sigOut0
          case  90 => TransposeMatrix(sigOut0, rows = height, columns = width)
          case -90 =>
            val r1 = TransposeMatrix(sigOut0, rows = height, columns = width )
            ReverseWindow(r1, frameSize)
          case 180 => ReverseWindow(sigOut0, frameSize)
        }
        val sigOut = if (!autoLevels) sigOut1 else {
          val minDropC  = RunningMin(sigOut1, Metro(frameSize)).drop(frameSize - 1)
          val maxDropC  = RunningMax(sigOut1, Metro(frameSize)).drop(frameSize - 1)
          val minDrop   = (minDropC \ 0).min(minDropC \ 1).min(minDropC \ 2)
          val maxDrop   = (maxDropC \ 0).max(maxDropC \ 1).max(maxDropC \ 2)
          val min       = Gate(minDrop, Metro(frameSize))
          val max       = Gate(maxDrop, Metro(frameSize))
          val el        = sigOut1.elastic((frameSizeL * 2) / streamCfg.blockSize + 1)
          (el - min) / (max - min)
        }

        val isHalfRot = rotate == 90 || rotate == -90
        val spec  = ImageFile.Spec(
          width  = if (isHalfRot) height else width,
          height = if (isHalfRot) width  else height,
          numChannels = 3,
          fileType = tpe,
          sampleFormat = ImageFile.SampleFormat.Int8, quality = 100
        )
//        val indicesOut  = idxRangeOut.map(x => x: GE).reduce(_ ++ _)
        val indicesOut  = ArithmSeq(idxRangeOut.head, step = 1, length = idxRangeOut.size)
        ImageFileSeqOut(template = templateOut, spec = spec, in = sigOut, indices = indicesOut)
        Progress(Frames(sigOut) / (frameSizeL * idxRangeOut.size), Metro(frameSize))

      } else {
        val last  = expose.take(frameSizeL * (numInput - (medianLen - 1))).takeRight(frameSize)
        // min.take(frameSize * (numInput - (medianLen - 1))).takeRight(frameSize)
        val sig   = normalize(last)
        val tpe   = if (fOut.ext == "png") ImageFile.Type.PNG else ImageFile.Type.JPG
        val spec  = ImageFile.Spec(width = width, height = height, numChannels = /* 1 */ 3,
          fileType = tpe, sampleFormat = ImageFile.SampleFormat.Int8,
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