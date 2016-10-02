/*
 *  ExtractWords.scala
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

import java.awt.{AlphaComposite, BasicStroke, Color, Shape}
import java.awt.geom.{AffineTransform, Area, Path2D, Rectangle2D}
import java.awt.image.BufferedImage
import java.util.Locale
import javax.imageio.ImageIO

import de.sciss.file._
import de.sciss.numbers
import org.apache.batik.parser.PathParser
import scopt.OptionParser

import scala.xml.{NodeSeq, XML}

object ExtractWords {
  case class Config(svgIn     : File    = file("in.svg"),
                    imageIn   : File    = file("in.jpg"),
                    outTemp   : File    = file("out-%d.png"),
                    imageScale: Double  = 5,
                    fadeSize  : Int     = 8,
                    background: Int     = 0x00FFFFFF,
                    overwrite : Boolean = false
                   )

  def main(args: Array[String]): Unit = {
    val parser = new OptionParser[Config]("ExtractWords") {
      opt[File  ]("svg-in") required() text "Annotated SVG input file"  action { (x, c) => c.copy(svgIn = x) }
      opt[File  ]("image-in") required() text "Image (high-res) input file" action { (x, c) => c.copy(imageIn = x) }
      opt[File  ]('d', "output-template") required() text "Output template" action { (x, c) => c.copy(outTemp = x) }
      opt[Double]("scale") text "Svg-to-image scale factor (default: 5)" action { (x, c) =>
        c.copy(imageScale = x)
      }
      opt[Unit  ]("overwrite") required() text "Overwrite existing files" action { (_, c) => c.copy(overwrite = true) }
      opt[Int   ]("background") text "Background color (default: 0x00FFFFFF" action { (x, c) =>
        c.copy(background = x)
      }
    }
    parser.parse(args, Config()).fold(sys.exit(1))(run)
  }

  case class Word(idx: Int, path: Path2D, bounds: Rectangle2D, baseline: Double, erase: Boolean)

  def run(config: Config): Unit = {
    import config._

    val fmt = outTemp.ext.toLowerCase(Locale.US)
    require (fmt == "png" || fmt == "jpg", s"Unsupported image format '$fmt'")

    val xmlRoot   = XML.load(svgIn.toURI.toURL)
    val pathsXML  = xmlRoot.\\("path")
    println(s"Seeing ${pathsXML.size} paths.")
    val (pathsBaselineXML, pathsWordXML) = pathsXML.partition(_.\("@style").text.contains("stroke:#0000ff"))
    println(s"Seeing ${pathsWordXML.size} word paths, ${pathsBaselineXML.size} pathsBaseline paths.")

    val pathsWord       = mkPaths(pathsWordXML)
    val erased          = pathsWordXML.map(n => (n\"@notebook").text.contains("-erase")).toVector
    val boundsWord      = pathsWord.map(_.getBounds2D)
    val pathsBaseline   = mkPaths(pathsBaselineXML)
    val boundsBaseline  = pathsBaseline.map(_.getBounds2D)
    val baselineHeight  = boundsBaseline.map(_.getHeight).max
    if (baselineHeight != 1.0)
      println(s"WARNING: Baseline shapes should all have height <= 1.0 (but found $baselineHeight).")
    val baselineIndices = boundsBaseline.map { r =>
      val cx = r.getCenterX
      val cy = r.getCenterY
      // note: contains with a rectangle of height zero always returns false!
      boundsWord.indexWhere(_.contains(cx, cy))
    }
    require (!baselineIndices.contains(-1), "Not all baselines could be attributed")
    require (baselineIndices == baselineIndices.sorted, "Baselines should appear in linear order")

//    println("---- words ----")
//    println(boundsWord.map(r => f"Rect(${r.getX}, ${r.getY}, ${r.getWidth}, ${r.getHeight})").mkString("\n"))
//    println("---- baselines ----")
//    println(boundsBaseline.map(r => f"Rect(${r.getX}, ${r.getY}, ${r.getWidth}, ${r.getHeight})").mkString("\n"))
//    println(baselineIndices.mkString(", "))

    val baselineMap = (baselineIndices zip boundsBaseline.map(_.getCenterY)).toMap
    val words = pathsWord.zip(boundsWord).zip(erased).zipWithIndex.map { case (((path, bPath), er), pIdx) =>
      val baseline = baselineMap.getOrElse(pIdx, {
        val predIdx = (pIdx to 0 by -1).find(baselineMap.contains).get
        val nextIdx = (pIdx until pathsWord.size).find(baselineMap.contains).get
        val pred = baselineMap(predIdx)
        val next = baselineMap(nextIdx)
        import numbers.Implicits._
        pIdx.linlin(predIdx, nextIdx, pred, next)
      })
      Word(pIdx, path, bPath, baseline = baseline, erase = er)
    }

    val ascent  = words.map(w => w.baseline - w.bounds.getMinY).max * imageScale
    val descent = words.map(w => w.bounds.getMaxY - w.baseline).max * imageScale
    val imgOutH = math.ceil(ascent + descent).toInt + fadeSize
    println(s"ascent $ascent, descent $descent, imgOutH $imgOutH")

    require (imageIn.isFile, s"Input image '$imageIn' not found.")
    val bufImgIn  = ImageIO.read(imageIn)
    println("Read input image.")
    // val gIn    = bufImgIn.createGraphics()
    val at        = new AffineTransform

    val fadeSizeH = 0.5 * fadeSize

    words.foreach { w =>
      val outF = formatFile(outTemp, w.idx + 1)
      if (!overwrite && outF.exists()) {
        println(s"Skipping '$outF' - file already exists")
      } else {
        val imgOutW   = math.ceil(w.bounds.getWidth * imageScale).toInt + fadeSize
        val bufImgOut = new BufferedImage(imgOutW, imgOutH, BufferedImage.TYPE_INT_ARGB)
        val gOut      = bufImgOut.createGraphics()
        gOut.setColor(new Color(background, true))
        gOut.setComposite(AlphaComposite.Src)
        gOut.fillRect(0, 0, imgOutW, imgOutH)
        gOut.setComposite(AlphaComposite.SrcOver)
        at.setToIdentity()
        at.scale     (imageScale, imageScale)
        at.translate (-w.bounds.getMinX, -w.baseline /* -w.bounds.getMinY */)
        val scaled    = at.createTransformedShape(w.path)
        at.setToTranslation(fadeSizeH, imgOutH - descent /* + descent0 */ + fadeSizeH)
        val basicShape = at.createTransformedShape(scaled)

        def mkFadeShape(fd: Int, in: Shape): Area = {
          val aIn = new Area(in)
          if (fd < fadeSizeH) {
            val strk = new BasicStroke((fadeSizeH - fd).toFloat * 2)
            val in1 = strk.createStrokedShape(in)
            aIn.subtract(new Area(in1))
          } else if (fd > fadeSizeH) {
            val strk = new BasicStroke((fd - fadeSizeH).toFloat * 2)
            val in1 = strk.createStrokedShape(in)
            aIn.add(new Area(in1))
          }
          aIn
        }

        var clip0: Area = null
        if (!w.erase) for (fd <- 0 until fadeSize) {
          import numbers.Implicits._
          val alpha = fd.linlin(0, fadeSize, 1.0, 0.0)
          val clip1 = mkFadeShape(fd, basicShape)
          val clip2 = if (clip0 == null) clip1 else {
            val res = new Area(clip1)
            res.subtract(clip0)
            res
          }
          gOut.setClip(clip2)
//          val colr = new Color(0xFF, 0, 0, (alpha * 0xFF).toInt)
//          gOut.setColor(colr)
//          gOut.fillRect(0, 0, imgOutW, imgOutH)
          val tx0 = -w.bounds.getMinX * imageScale
          val tx  = tx0 + fadeSizeH
          val ty0 = -w.baseline * imageScale
          val ty  = ty0 + imgOutH - descent + fadeSizeH
          at.setToTranslation(tx, ty)
          val comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.toFloat)
          gOut.setComposite(comp)
          gOut.drawImage(bufImgIn, at, null)
          clip0 = clip1
        }

        ImageIO.write(bufImgOut, fmt, outF)
        gOut.dispose()
        bufImgOut.flush()
      }
    }

    bufImgIn.flush()
  }

  def mkPaths(in: NodeSeq): Vector[Path2D] = {
    val parser = new PathParser
    in.map { n =>
      val path  = (n \ "@d").text
      val h     = new Path2DHandler
      parser.setPathHandler(h)
      parser.parse(path)
      h.result()
    } .toVector
  }
}