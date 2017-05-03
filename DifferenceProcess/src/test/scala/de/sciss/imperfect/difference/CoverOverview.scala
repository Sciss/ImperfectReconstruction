package de.sciss.imperfect.difference

import de.sciss.file._
import de.sciss.fscape.stream.Control
import de.sciss.fscape.{GE, Graph, graph}

object CoverOverview {
  val baseDirInt: File = file("/") / "data" / "projects" / "Imperfect"
  val coverDir  : File = baseDirInt / "catalog" / "cover"
  val pngInTemp : File = coverDir / "front" / "front-%d.jpg"
  val fOut      : File = coverDir / "assemblage.jpg"

  def main(args: Array[String]): Unit = {
    if (fOut.exists() && fOut.length() > 0L) {
      println(s"File $fOut already exists. Not overwriting.")
    } else {
      run()
    }
  }

  def run(): Unit = {
    val num   = 300
    val cols  = 12 // 15
    val rows  = 25 // 20
    require(cols * rows == num)
    val widthIn   = 3252
    val heightIn  = 1638
    val factor    = 2 * 3
    val scale     = 1.0 / factor
    val widthOut  = widthIn  / factor
    val heightOut = heightIn / factor

    val g = Graph {
      import graph._

      val in      = ImageFileSeqIn(pngInTemp, numChannels = 3, indices = ArithmSeq(start = 1, length = 300))
      val scaled  = AffineTransform2D.scale(in, widthIn, heightIn, widthOut, heightOut,
        sx = scale, sy = scale, zeroCrossings = 3)

      val totalWidth  = widthOut  * cols
      val totalHeight = heightOut * rows
      val sizeOut     = widthOut * heightOut

      val tr  = {
        val x = Vector.tabulate(3) { ch =>
          val inC = scaled \ ch
          val u   = UnzipWindowN(in = inC, size = sizeOut, numOutputs = cols)
          val e   = u.elastic(heightOut)
          val z   = ZipWindowN  (in = e  , size = widthOut)
          z
        }
        x: GE
      }

      val framesOut   = totalWidth.toLong * totalHeight
      val framesOutP  = framesOut / 100.0
      (Frames(tr \ 0) / framesOutP).ceil.poll(Metro(framesOutP), "progress")

      val specOut = ImageFile.Spec(ImageFile.Type.JPG, width = totalWidth, height = totalHeight, numChannels = 3)
      val sigOut = tr.max(0).min(1)
      ImageFileOut(fOut, specOut, in = sigOut)
    }

    val cfg = Control.Config()
    cfg.blockSize = widthIn
    val ctrl = Control(cfg)
    ctrl.run(g)
  }
}