/*
 *  MakeCatalogCover.scala
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

import java.awt.{AlphaComposite, Color}
import java.awt.image.BufferedImage
import java.io.{FileInputStream, FileOutputStream}
import javax.imageio.metadata.{IIOMetadata, IIOMetadataNode}
import javax.imageio.plugins.jpeg.JPEGImageWriteParam
import javax.imageio.{IIOImage, ImageIO, ImageTypeSpecifier, ImageWriteParam}

import de.sciss.file._

object MakeCatalogCover {
  val baseDirInt: File = file("/") / "data" / "projects" / "Imperfect"
//  val baseDirExt: File = file("/media") / "hhrutz" / "AC6E5D6F6E5D3376" / "projects" / "Imperfect"
  val coverDir  : File = baseDirInt / "catalog" / "cover"

  val mainDPI           : Int     = 200
  val _davidDPI         : Int     = 116
  val _sheetWidth       : Double  = 420.0
  val _sheetHeight      : Double  = 215.0
  val _sheetCutLeft     : Double  = 3.5 // 6.0 // 7.5
  val _sheetCutRight    : Double  = 3.5 // 6.0 // 7.5
  val _sheetInsetLeft   : Double  = 7.0
  val _sheetInsetRight  : Double  = 7.0
  val _sheetCutTop      : Double  = 3.5
  val _sheetWidthInner  : Double  = _sheetWidth - (_sheetInsetLeft + _sheetInsetRight)

  implicit class NumOps(d: Double) {
    def mmToInches: Double = d / 25.4
  }

  case class Config(
                     davidDir         : File    = baseDirInt / "david" / "causality_report",
                     hhDir            : File    = coverDir / "site-2out_catalogSel",
                     hhExt            : String  = "jpg",
                     pngOutTemp       : File    = coverDir / "front" / "front-%d.jpg",
                     pdfOutTemp       : File    = coverDir / "front-pdf" / "front-%d.pdf",
                     cropMarks        : File    = coverDir / "cover_white.pdf",
                     titleText        : File    = coverDir / "stamp.pdf",
                     strokeWidth      : Double  = 2.0,
                     compositionGamma : Double  = 1.6, // 2.0,
                     innerWidth       : Int     = ((_sheetWidthInner/2).mmToInches * mainDPI).round.toInt & ~1,
                     davidDPI         : Double  = _davidDPI,
                     hhDPI            : Int     = mainDPI,
                     tag              : Boolean = true,
                     tagWidth         : Int     = 6,
                     tagHeight        : Int     = 18,
                     tagMarginRight   : Int     = (6.5 / 25.4 * mainDPI + 0.5).toInt,
                     tagMarginBottom  : Int     = (6.5 / 25.4 * mainDPI + 0.5).toInt,
                     renderPDF        : Boolean = true,
                     assembly         : Option[File] = Some(coverDir / "front-all.pdf"),
                     maxItems         : Int     = -1,
                     sheetWidth       : Double  = _sheetWidth,
                     sheetHeight      : Double  = _sheetHeight,
                     sheetCutTop      : Double  = _sheetCutTop,
                     sheetCutLeft     : Double  = _sheetCutLeft,
                     sheetCutRight    : Double  = _sheetCutRight,
                     tagMaxRed        : Int     = 177, // 213,
                     tagMaxGreen      : Int     = 255,
                     tagMaxBlue       : Int     = 177  // 213
                   )

  def main(args: Array[String]): Unit = {
    run(Config())
  }

  implicit val fileNameOrdering = new Ordering[File] {
    def compare(f1: File, f2: File): Int = {
      compareName(f1.name, f2.name)
//      foo.Foo.compareNames(f1.name, f2.name)
    }
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

    val davidIn0    = davidDir.children(_.ext == "svg").sorted[File]
    val hhIn0       = hhDir   .children(_.ext == hhExt)
//    println(s"size = ${hhIn0.size}")
    val hhIn1       = hhIn0.sorted[File]
    val davidIn     = davidIn0 // .take(1)
    val hhIn        = hhIn1    // .take(1)
    val compDavid   = new MultiplyGammaComposite(gamma = compositionGamma.toFloat)
    val compTag     = new MultiplyGammaComposite(maxRed = tagMaxRed, maxGreen = tagMaxGreen, maxBlue = tagMaxBlue)
    val compNorm    = AlphaComposite.SrcOver
    val colrWhite50 = new Color(0xFF, 0xFF, 0xFF, 0x7F)
    val colrBlack50 = new Color(0x00, 0x00, 0x00, 0x7F)
    val th3         = tagHeight/3
    val th6         = tagHeight/6
    val tw2         = tagWidth /2

    //    outTemp.parentOption.foreach(_.mkdirs())
    val zipped0   = davidIn zip hhIn
    if (zipped0.isEmpty) {
      println("Woops. No input files found.")
    }

    val zipped    = if (maxItems > 0) zipped0.take(maxItems) else zipped0

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
        g2.setComposite(compDavid)
        g2.drawImage(resized, ix, 0, null)

        if (tag) {
          // g2.setClip(clipOrig)
          for (i <- 0 until 9) {
            g2.setComposite(compTag)
            val b   = ((frameIdx >>> i) & 1) == 1
            val px1 = width - (tagWidth * 2 * (i + 1)) - tagMarginRight
            val px2 = px1 + tagWidth
            val ch  = if (b) tagHeight else th3
            val py1 = height - tagMarginBottom - ch
            val py2 = py1 + ch
            g2.drawImage(hhPng, px1, py1, px2, py2, px1, py1, px2, py2, null)

            g2.setComposite(compNorm)
            g2.setColor(colrWhite50)
            g2.drawLine(px1, py1 + th6, px1, py1)
            g2.drawLine(px1, py1, px1 + tw2, py1)
            g2.setColor(colrBlack50)
            val px2m = px2 - 1
//            val py2m = py2 - 1
//            g2.drawLine(px2m, py2m - th6, px2m, py2m)
//            g2.drawLine(px2m, py2m, px2m - tw2, py2m)
            g2.drawLine(px2m, py1 + th6, px2m, py1)
            g2.drawLine(px2m, py1, px2m - tw2, py1)
          }
        }
        g2.dispose()

        // ImageIO.write(hhPng, "png", composedF)
        val imageFormat = composedF.ext.toLowerCase match {
          case "png"          => "png"
          case "jpg" | "jpeg" => "jpg"
        }
        saveImageWithDPI(hhPng, composedF, dpi = hhDPI, format = imageFormat)

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

        // bloody includegraphics performs scaling of trim values depending
        // on image scale. so in the case of jpg which doesn't store dpi
        // settings, we need to correcgt with a factor of nominal-dpi/72
        val sheetCutTop1 = if (composedF.ext == "png") sheetCutTop else sheetCutTop * mainDPI / 72.0

        val tex =
          f"""\\documentclass{article}
             |\\usepackage[paperwidth=$sheetWidth%gmm,paperheight=$sheetHeight%gmm,top=0mm,left=0mm,bottom=0mm,right=0mm]{geometry}
             |\\usepackage{graphicx}
             |\\usepackage{background}
             |\\backgroundsetup{
             |scale=1,
             |angle=0,
             |contents={%%
             |\\includegraphics[width=\\paperwidth,height=\\paperheight]{${cropMarks.path}}%%
             |}}
             |\\begin{document}
             |\\thispagestyle{empty}
             |\\centering
             |\\includegraphics[width=${sheetWidth - (sheetCutLeft + sheetCutRight)}%gmm,trim=0 0 0 -$sheetCutTop1%gmm]{${composedF.path}}%%
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

        val tempF     = File.createTemp(suffix = ".pdf")

        val cmdAss    = Seq("pdftk") ++ inPaths ++ Seq("cat", "output", tempF.path)
        val cmdStamp  = Seq("pdftk", tempF.path, "stamp", titleText.path, "output", assemblyF.path)

        import sys.process._
        try {
          Process(cmdAss  , wd).!!
          Process(cmdStamp, wd).!!
        } finally {
          tempF.delete()
        }

      } else {
        println(s"Skipping '${assemblyF.name}' - file already exists.")
      }
    }
  }

  // http://stackoverflow.com/questions/321736/how-to-set-dpi-information-in-an-image#4833697
  def saveImageWithDPI(img: BufferedImage, f: File, dpi: Int, format: String = "png"): Unit = {
    if (f.exists()) require(f.delete())

    import scala.collection.JavaConverters._
    val (writer, metadata, writeParam) = ImageIO.getImageWritersByFormatName(format).asScala.flatMap { _writer =>
      val _wp           = _writer.getDefaultWriteParam
      val typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB)
      val _md = _writer.getDefaultImageMetadata(typeSpecifier, _wp)
      if (_md.isReadOnly || !_md.isStandardMetadataFormatSupported) None
      else Some((_writer, _md, _wp))
    } .next()

    setDPI(metadata, dpi)

    writeParam match {
      case p: JPEGImageWriteParam =>
//        val p = new JPEGImageWriteParam(null)
        p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
        p.setCompressionQuality(0.99f)
//        p
      case _ =>
    }

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
          if (c1 == c2 && c3 != c4) {
            c1 = c3
            c2 = c4
          }
          d1 = Character.isDigit(c3)
          d2 = Character.isDigit(c4)
        }
        while (d1 && d2)

        if (d1 != d2) return if (d1) 1 else -1
        if (c1 != c2) return c1 - c2
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