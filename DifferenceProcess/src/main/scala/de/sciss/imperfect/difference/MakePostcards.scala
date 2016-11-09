package de.sciss.imperfect.difference

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.{FileInputStream, FileOutputStream}
import javax.imageio.ImageIO

import de.sciss.file._

object MakePostcards {
  val baseDir = userHome / "Documents" / "projects" / "Imperfect"

  case class Config(
                   davidDir   : File    = baseDir / "david" / "causality_report",
                   hhDir      : File    = baseDir / "site-2out_cover_final",
                   outTemp    : File    = baseDir / "logistics" / "flyer" / "front" / "front-%d.png",
                   strokeWidth: Double  = 2.0,
                   gamma      : Double  = 2.0,
                   innerWidth : Int     = 816, // 826
                   davidDPI   : Double  = 59
                 )

  def main(args: Array[String]): Unit = {
    run(Config())
  }

  implicit val fileNameOrdering = new Ordering[File] {
    def compare(f1: File, f2: File): Int = compareName(f1.name, f2.name)
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

  def applyTemplate(temp: File, idx: Int): File = {
    val name = temp.name.format(idx)
    temp.parentOption.fold(file(name))(_ / name)
  }

  def run(config: Config): Unit = {
    import config._

    // change stroke width to 2.0
    // convert -density 60 in.svg out.png
    // then trunk 1 px left + right, 2 px top + bottom, then extend to
    // target height (850 == 108 mm at 200 dpi)

    val davidIn0  = davidDir.children(_.ext == "svg").sorted
    val hhIn0     = hhDir   .children(_.ext == "png").sorted
    val davidIn   = davidIn0.take(1)
    val hhIn      = hhIn0   .take(1)
    val compGamma = new MultiplyGammaComposite(gamma = gamma.toFloat)

//    outTemp.parentOption.foreach(_.mkdirs())

    (davidIn zip hhIn).zipWithIndex.foreach { case ((svgIn, siteIn), frameIdx) =>
      val composedF = applyTemplate(outTemp, frameIdx + 1)

      if (!composedF.exists()) {
        println(s"Rendering... '${composedF.name}'")

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
        g2.clipRect(ix, 0, innerWidth, height)
        g2.setComposite(compGamma)
        g2.drawImage(resized, ix, 0, null)
        g2.dispose()

        ImageIO.write(hhPng, "png", composedF)
        resized.flush()
        hhPng  .flush()
        pngOutF .delete()
        svgOutF .delete()
      } else {
        println(s"Skipping '${composedF.name}' - file already exists.")
      }
    }
  }
}
