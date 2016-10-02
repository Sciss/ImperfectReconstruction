package de.sciss.imperfect.notebook

import de.sciss.file._
import de.sciss.fscape.graph.Concat
import de.sciss.numbers
import de.sciss.fscape.stream.Control
import de.sciss.fscape.{GE, Graph, graph}

import scala.Predef.{any2stringadd => _, _}

object TransitionTest {
  def main(args: Array[String]): Unit = run()

  def run(): Unit = {
    val snow = Graph {
      import graph._
      val dir     = userHome/"Documents"/"projects"/"Imperfect"/"scans"/"notebook2016"
      val fIn1    = dir / "universe-test.png"
      val fIn2    = dir / "universe-test2.png"
      //      val fOut    = dir / "test-out" / "universe-test-%d.png"
      val fOut    = dir / "universe-test-out.png"
      val in1     = ImageFileIn(fIn1, numChannels = 3)
      //      val in2     = ImageFileIn(fIn2, numChannels = 3)
      //      val fftSize = 1024 * 1024
      //      val fft1    = Real1FFT(in1, size = fftSize, mode = 2)
      //      val fft2    = Real1FFT(in2, size = fftSize, mode = 2)
      ////      val mul     = (fft1.complex.exp.complex * fft2.complex.exp).complex.log
      ////      val mul     = fft1 max fft2
      //      val mag1    = fft1.complex.mag
      //      val phase1  = fft1.complex.phase
      //      val mag2    = fft2.complex.mag
      //      val phase2  = fft2.complex.phase
      //      val mag3    = (mag1   + mag2  ) / 2
      //      val phase3  = (phase1 + phase2) / 2
      //      val re      = phase3.cos * mag3
      //      val im      = phase3.sin * mag3
      //      val mul     = re zip im
      //      val sig     = Real1IFFT(mul, size = fftSize, mode = 2).max(0).min(1)
      //      val sig0 = GramSchmidtMatrix(in1, 1024, 1024, normalize = 0)

      //      val imgSize = 1024 * 1024
      //      val rsmp = ResampleWindow(in1 ++ BufferDisk(in1) ++ in2 ++ BufferDisk(in2), size = imgSize, factor = 24, minFactor = 24)
      //      val sig0 = rsmp // rsmp.drop(imgSize * 4).take(imgSize)

      def normalize(in: GE): GE = {
        val max = RunningMax(in).last
        BufferDisk(in) * max.reciprocal
      }

      import numbers.Implicits._

      val in3  = Real1FFT(in1, 1024 * 1024, mode = 0)
      val sig0 = Bleach(in3, filterLen = 32 /* 1024 */, feedback = -60.0 /* -48.0 */.dbamp)
      val sig1 = sig0 // in3.elastic() - sig0
      val sig2 = Real1IFFT(sig1, 1024 * 1024, mode = 0)

      //      val sig = sig1.max(0).min(1) // normalize(sig0)
      val sig = normalize(sig2)

      ImageFileOut(fOut, ImageFile.Spec(width = 1024, height = 1024, numChannels = 3), in = sig)
      //      val indices = ((1: GE) /: (2 to 48)) { case (in, x) => Concat(in, x) }
      //      ImageFileSeqOut(fOut, ImageFile.Spec(width = 1024, height = 1024, numChannels = 3), indices = indices, in = sig)
    }

    def mkSeqI(in: Seq[Int   ]): GE = ((in.head: GE) /: in.tail) { case (res, x) => Concat(res, x) }
    def mkSeqL(in: Seq[Long  ]): GE = ((in.head: GE) /: in.tail) { case (res, x) => Concat(res, x) }
    def mkSeqD(in: Seq[Double]): GE = ((in.head: GE) /: in.tail) { case (res, x) => Concat(res, x) }

    val g = Graph {
      import graph._
      val dir     = userHome/"Documents"/"projects"/"Imperfect"/"scans"/"notebook2016"
      val fIn1    = dir / "universe-test.png"
      val fIn2    = dir / "universe-test2.png"
      val fOutT   = dir / "test-out" / "universe-test-%d.png"
      val fOut    = dir / "universe-test-out.png"
      val in1     = ImageFileIn(fIn1, numChannels = 3)

      def normalize(in: GE): GE = {
        val max = RunningMax(in).last
        BufferDisk(in) * max.reciprocal
      }

      import numbers.Implicits._

//      val imageSize = 1024L * 1024L
//      val spans = mkSeqL(Seq(0L ))
//      Slices(in1, spans)

      val in3  = Real1FFT(in1, 1024 * 1024, mode = 0)
      val sig0 = Bleach(in3, filterLen = 32 /* 1024 */, feedback = -60.0 /* -48.0 */.dbamp)
      val sig1 = sig0 // in3.elastic() - sig0
      val sig2 = Real1IFFT(sig1, 1024 * 1024, mode = 0)

//      val sig = sig1.max(0).min(1) // normalize(sig0)
      val sig = normalize(sig2)

      ImageFileOut(fOut, ImageFile.Spec(width = 1024, height = 1024, numChannels = 3), in = sig)
      val indices = mkSeqI(1 to 48)
      ImageFileSeqOut(fOutT, ImageFile.Spec(width = 1024, height = 1024, numChannels = 3), indices = indices, in = sig)
    }

    val cc  = Control.Config()
    val ctl = Control(cc)
    ctl.run(g)

    import scala.concurrent.ExecutionContext.Implicits.global
    ctl.status.onComplete { x =>
      println(s"Result: $x")
      // sys.exit()
    }
  }
}
