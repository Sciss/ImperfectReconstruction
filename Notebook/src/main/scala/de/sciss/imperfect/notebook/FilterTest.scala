package de.sciss.imperfect.notebook

import de.sciss.file._
import de.sciss.synth.io.{AudioFile, AudioFileSpec}
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_2D

/** Just to verify we're doing the right translation Convolve.scala > ConvolveFSc.scala (result: yes, we do) */
object FilterTest extends App {
  val kernel  = 16
  val kh      = kernel/2
  val fft     = new DoubleFFT_2D(kernel, kernel)
  val inF: File = userHome / "Documents" / "projects" / "Imperfect" / "scans" / "notebook2016" / "hp5.aif"
  val afIn  = AudioFile.openRead(inF)
  val afb = afIn.buffer(afIn.numFrames.toInt)
  afIn.read(afb)
  afIn.close()
  val flt = afb(0)

  val fltLen  = flt.length
  val fltLenH = fltLen >> 1
  val bFlt = Array.tabulate(kernel) { y =>
    Array.tabulate(kernel) { x =>
      val x0 = (if (x > kh) x - kernel else x) + fltLenH
      val y0 = (if (y > kh) y - kernel else y) + fltLenH
      if (x0 >= 0 && y0 >= 0 && x0 < fltLen && y0 < fltLen)
        flt(x0) * flt(y0)
      else
        0.0
    }
  }
  fft.realForward(bFlt)
  //    val afTemp = AudioFile.openWrite(userHome/"Documents"/"temp"/"filt-fft.aif", AudioFileSpec(numChannels = 1, sampleRate = 44100))
  //    afTemp.write(Array(bFlt.flatten.map(_.toFloat)))
  //    afTemp.close()

  val outF: File = userHome / "Documents" / "temp" / "test.aif"
  val afOut = AudioFile.openWrite(outF, AudioFileSpec(numChannels = 1, sampleRate = 44100))
  val data = bFlt.flatMap(_.map(_.toFloat))
  afOut.write(Array(data))
  afOut.close()
}