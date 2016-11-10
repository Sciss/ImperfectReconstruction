/*
 *  MakePostcards.scala
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

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.{FileInputStream, FileOutputStream}
import javax.imageio.metadata.{IIOMetadata, IIOMetadataNode}
import javax.imageio.{IIOImage, ImageIO, ImageTypeSpecifier}

import de.sciss.file._

object MakePostcards {
  val baseDirInt    = userHome / "Documents" / "projects" / "Imperfect"
  val baseDirExt    = file("/media") / "hhrutz" / "AC6E5D6F6E5D3376" / "projects" / "Imperfect"
  val flyerDir      = baseDirExt / "logistics" / "flyer"

  case class Config(
                     davidDir   : File    = baseDirInt / "david" / "causality_report",
                     // hhDir   : File    = baseDir / "site-2out_cover_final",
                     hhDir      : File    = baseDirExt / "site-2out_cover_final",
                     pngOutTemp : File    = flyerDir / "front" / "front-%d.png",
                     pdfOutTemp : File    = flyerDir / "front-pdf" / "front-%d.pdf",
                     cropMarks  : File    = flyerDir / "postcard-front-empty.pdf",
                     strokeWidth: Double  = 2.0,
                     gamma      : Double  = 2.0,
                     innerWidth : Int     = 816, // 826
                     davidDPI   : Double  = 59,
                     hhDPI      : Int     = 200,
                     tag        : Boolean = true,
                     tagWidth   : Int     = 5,
                     tagHeight  : Int     = 15,
                     tagMargin  : Int     = (6 / 25.4 * 200 + 0.5).toInt,
                     renderPDF  : Boolean = true,
                     assembly   : Option[File] = Some(flyerDir / "front-all.pdf")
                 )

  def main(args: Array[String]): Unit = {
    run(Config())
  }

  implicit val fileNameOrdering = new Ordering[File] {
    def compare(f1: File, f2: File): Int = compareName(f1.name, f2.name)
  }

  def applyTemplate(temp: File, idx: Int): File = {
    val name = temp.name.format(idx)
    temp.parentOption.fold(file(name))(_ / name)
  }

  def run(config: Config): Unit = {
    import config._

    require(hhDir   .isDirectory, s"Not found: ${hhDir   .path}")
    require(davidDir.isDirectory, s"Not found: ${davidDir.path}")
    require(pngOutTemp .parentOption.exists(_.isDirectory), s"Not found: ${pngOutTemp.path}")

    // change stroke width to 2.0
    // convert -density 60 in.svg out.png
    // then trunk 1 px left + right, 2 px top + bottom, then extend to
    // target height (850 == 108 mm at 200 dpi)

    val davidIn0  = davidDir.children(_.ext == "svg").sorted
    val hhIn0     = hhDir   .children(_.ext == "png").sorted
    val davidIn   = davidIn0 // .take(1)
    val hhIn      = hhIn0    // .take(1)
    val compGamma = new MultiplyGammaComposite(gamma = gamma.toFloat)
    val compMul   = new MultiplyGammaComposite

//    outTemp.parentOption.foreach(_.mkdirs())
    val zipped    = davidIn zip hhIn

    zipped.zipWithIndex.foreach { case ((svgIn, siteIn), frameIdx0) =>
      val frameIdx  = frameIdx0 + 1
      val composedF = applyTemplate(pngOutTemp, frameIdx)

      // -------- png --------

      if (!composedF.exists()) {
        println(s"Rendering... '${composedF.name}' ('${svgIn.name}')")

        val fIn = new FileInputStream(svgIn)
        val byteIn = new Array[Byte](fIn.available())
        fIn.read(byteIn)
        fIn.close()
        val svgInS = new String(byteIn, "UTF-8")
        val strkIn = "stroke-width:0.5"
        val i = svgInS.indexOf(strkIn)
        require (i >= 0)
        val strkOut = s"stroke-width:$strokeWidth"
        val svgOutS = svgInS.substring(0, i) + strkOut + svgInS.substring(i + strkIn.length)
        val svgOutF = File.createTemp(suffix = ".svg")
        val fOut = new FileOutputStream(svgOutF)
        fOut.write(svgOutS.getBytes("UTF-8"))
        fOut.close()

        import sys.process._
        // image-magick
        val pngOutF     = File.createTemp(suffix = ".png")
        val cmdSvgToPng = Seq("convert", "-density", davidDPI.toString, svgOutF.path, pngOutF.path)
        cmdSvgToPng.!!

        val davidPng = ImageIO.read(pngOutF)
        val hhPng    = ImageIO.read(siteIn)
        val width    = hhPng.getWidth
        val height   = hhPng.getHeight
        val resized  = new BufferedImage(innerWidth, height, BufferedImage.TYPE_INT_RGB)
        val g1 = resized.createGraphics()
        g1.setColor(Color.black)
        g1.fillRect(0, 0, innerWidth, height)
        val dTrimLeft   = 2
        // val dTrimRight  = 1
        val dTrimTop    = 2
        val dTrimBottom = 2
        val dx1 = 0
        val davidHeightCrop = davidPng.getHeight - (dTrimTop + dTrimBottom)
        val dy1 = (height - davidHeightCrop) >> 1
        val dx2 = dx1 + innerWidth
        val dy2 = dy1 + davidHeightCrop
        val sx1 = dTrimLeft
        val sy1 = dTrimTop
        val sx2 = sx1 + innerWidth
        val sy2 = sy1 + davidHeightCrop
        g1.drawImage(davidPng, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null)
        g1.dispose()
        davidPng.flush()

        val g2 = hhPng.createGraphics()
        val ix = (hhPng.getWidth - innerWidth) >> 1
        // val clipOrig = g2.getClip
        // g2.clipRect(ix, 0, innerWidth, height)
        g2.setComposite(compGamma)
        g2.drawImage(resized, ix, 0, null)

        if (tag) {
          g2.setComposite(compMul)
          // g2.setClip(clipOrig)
          for (i <- 0 until 9) {
            val b   = ((frameIdx >>> i) & 1) == 1
            val px1 = width - (tagWidth * 2 * (i + 1)) - tagMargin
            val px2 = px1 + tagWidth
            val ch  = if (b) tagHeight else tagHeight/3
            val py1 = height - tagMargin - ch
            val py2 = py1 + ch
            g2.drawImage(hhPng, px1, py1, px2, py2, px1, py1, px2, py2, null)
          }
        }
        g2.dispose()

        // ImageIO.write(hhPng, "png", composedF)
        saveImageWithDPI(hhPng, composedF, dpi = hhDPI)

        resized.flush()
        hhPng  .flush()
        pngOutF .delete()
        svgOutF .delete()
      } else {
        println(s"Skipping '${composedF.name}' - file already exists.")
      }

      // -------- pdf --------

      val pdfF = applyTemplate(pdfOutTemp, frameIdx)
      if (renderPDF && !pdfF.exists()) {
        println(s"Rendering... '${pdfF.name}'")

        val tex =
          s"""\\documentclass{article}
             |\\usepackage[paperwidth=227mm,paperheight=117mm,top=0mm,left=0mm,bottom=0mm,right=0mm]{geometry}
             |\\usepackage{graphicx}
             |\\usepackage{background}
             |\\backgroundsetup{
             |scale=1,
             |angle=0,
             |contents={\\includegraphics[width=\\paperwidth,height=\\paperheight]{${cropMarks.path}}}
             |}
             |\\begin{document}
             |\\thispagestyle{empty}
             |\\centering
             |\\includegraphics[trim=0 0 0 -4.5mm]{${composedF.path}}
             |\\end{document}
             |""".stripMargin

        val pdfDir  = File.createTemp(directory = true)
        val texF    = pdfDir / "page.tex"
        val fOut    = new FileOutputStream(texF)
        fOut.write(tex.getBytes("UTF-8"))
        fOut.close()
        import sys.process._
        val cmdPDF = Seq("pdflatex", texF.name)
//        println(texF)
        // run `pdflatex` twice, otherwise background crop marks do not appear
        Process(cmdPDF, texF.parent).!!
        Process(cmdPDF, texF.parent).!!
        val cmdMove = Seq("mv", texF.replaceExt("pdf").path, pdfF.path)
        cmdMove.!!
        pdfDir.children.foreach(_.delete())
        pdfDir.delete()

      } else {
        if (renderPDF) println(s"Skipping '${pdfF.name}' - file already exists.")
      }
    }

    assembly.foreach { assemblyF =>
      if (!assemblyF.exists()) {
        println(s"Assembling... '${assemblyF.name}'")

        /*
        gs -sDEVICE=pdfwrite -dNOPAUSE -dBATCH -dSAFER -sOutputFile=combined.pdf first.pdf second.pdf ...
         */

        val inPaths = (1 to zipped.size).map(frameIdx => applyTemplate(pdfOutTemp, frameIdx).name)
        val wd      = pdfOutTemp.parent
//        val cmdAss  = Seq("gs", "-sDEVICE=pdfwrite", "-dPDFSETTINGS=/printer", "-dNOPAUSE", "-dBATCH", "-dSAFER",
//          s"-sOutputFile=${assemblyF.path}") ++ inPaths

        val cmdAss = "pdftk" +: inPaths :+ "cat" :+ "output" :+ assemblyF.path

        import sys.process._
        Process(cmdAss, wd).!!

      } else {
        println(s"Skipping '${assemblyF.name}' - file already exists.")
      }
    }
  }

  // http://stackoverflow.com/questions/321736/how-to-set-dpi-information-in-an-image#4833697
  def saveImageWithDPI(img: BufferedImage, f: File, dpi: Int, format: String = "png"): Unit = {
    if (f.exists()) require(f.delete())

    import scala.collection.JavaConversions._
    val (writer, metadata, writeParam) = ImageIO.getImageWritersByFormatName(format).flatMap { _writer =>
      val _wp           = _writer.getDefaultWriteParam
      val typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB)
      val _md = _writer.getDefaultImageMetadata(typeSpecifier, _wp)
      if (_md.isReadOnly || !_md.isStandardMetadataFormatSupported) None
      else Some((_writer, _md, _wp))
    } .next()

    setDPI(metadata, dpi)

    val stream = ImageIO.createImageOutputStream(f)
    try {
      writer.setOutput(stream)
      writer.write(metadata, new IIOImage(img, null, metadata), writeParam)
    } finally {
      stream.close()
    }
  }

  def setDPI(metadata: IIOMetadata, dpi: Int): Unit = {
    val dotsPerMilli = dpi / 2.54 / 10
    val horiz = new IIOMetadataNode("HorizontalPixelSize")
    horiz.setAttribute("value", java.lang.Double.toString(dotsPerMilli))
    val vert  = new IIOMetadataNode("VerticalPixelSize")
    vert.setAttribute("value", java.lang.Double.toString(dotsPerMilli))

    val dim = new IIOMetadataNode("Dimension")
    dim.appendChild(horiz)
    dim.appendChild(vert)

    val root = new IIOMetadataNode("javax_imageio_1.0")
    root.appendChild(dim)

    metadata.mergeTree("javax_imageio_1.0", root)
  }
  
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
        var c3, c4 = ' '
        do {
          i += 1
          c3 = if (i < n1) s1.charAt(i) else 'x'
          c4 = if (i < n2) s2.charAt(i) else 'x'
          d1 = Character.isDigit(c3)
          d2 = Character.isDigit(c4)
        }
        while (d1 && d2 && c3 == c4)

        if (d1 != d2) return if (d1) 1 else -1
        if (c1 != c2) return c1 - c2
        if (c3 != c4) return c3 - c4
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
}
