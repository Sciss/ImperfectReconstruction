/*
 *  PositionWords2.scala
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

import java.awt.{Color, RenderingHints}
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import de.sciss.file._
import de.sciss.kollflitz.Vec
import org.jdesktop.swingx.graphics.BlendComposite

import scala.annotation.tailrec

object PositionWords2 {
  def main(args: Array[String]): Unit = run()

  def run(): Unit = {
    val groups    = FindSentences.run(print = false)

    val baseDir   = userHome / "Documents" / "projects" / "Imperfect" / "scans" / "notebook2016"
    require(baseDir.isDirectory)
    val outDir    = baseDir / "universe-pages"
    val wordDir   = baseDir / "universe-words"

    val blendMul  = BlendComposite.Darken // .Multiply

    groups.zipWithIndex.foreach { case (sentences, gi) =>
      val groupDir = outDir / s"group-${gi + 1}"
      groupDir.mkdirs()

      val wordIndices = sentences.flatten.map(_._2)
      val numLines    = 3
      val pageWidth   = 1024
      val pageHeight  = 1024
      val scaleFactor = 0.4
      val lineHeight  = 459 - 176
      val wordSpacing = 16 // 24 // 48
      val offsetLeft  = 4
      val offsetTop   = 2

      @tailrec def loopPage(rem: Vec[Int], pageIdx: Int): Unit = if (rem.nonEmpty) {
        val img = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_ARGB)
        val g   = img.createGraphics()
        g.setColor(Color.white)
        g.fillRect(0, 0, pageWidth, pageHeight)
        g.setComposite(blendMul)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION , RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING     , RenderingHints.VALUE_RENDER_QUALITY)
        g.scale(scaleFactor, scaleFactor)

        @tailrec def loopLines(rem: Vec[Int], lineIdx: Int, maxX: Int): (Vec[Int], Int) =
          if (lineIdx == numLines || rem.isEmpty) (rem, maxX) else {
            val y = lineIdx * lineHeight + offsetTop

            @tailrec def loopLine(rem: Vec[Int], x: Int, maxX: Int): (Vec[Int], Int) =
              rem match {
                case head +: tail =>
                  val wordF     = wordDir / s"out-${head + 1}.png"
                  val imgWord   = ImageIO.read(wordF)
                  val imgWord2  = new BufferedImage(imgWord.getWidth, imgWord.getHeight, BufferedImage.TYPE_INT_ARGB)
                  val g2 = imgWord2.createGraphics()
                  g2.setColor(Color.white)
                  g2.fillRect(0, 0, imgWord.getWidth, imgWord.getHeight)
                  g2.drawImage(imgWord, 0, 0, null)
                  g2.dispose()
                  imgWord.flush()
                  val wordWidth = math.ceil(imgWord.getWidth * scaleFactor).toInt
                  val x1        = if (x == 0) offsetLeft else x
                  val xNext     = x1 + wordSpacing + wordWidth
                  if (x > 0 && xNext > pageWidth) {
                    imgWord2.flush()
                    (rem, maxX)
                  } else {
                    g.drawImage(imgWord2, math.round(x1 / scaleFactor).toInt, math.round(y / scaleFactor).toInt, null)
                    imgWord2.flush()
                    loopLine(tail, x = xNext, maxX = math.max(maxX, xNext))
                  }

                case _ => (rem, maxX)
              }

            val (rem1, maxX1) = loopLine(rem, x = 0, maxX = 0)
            loopLines(rem1, lineIdx = lineIdx + 1, maxX = math.max(maxX, maxX1))
          }

        val (rem1, maxXPage) = loopLines(rem, lineIdx = 0, maxX = 0)
        g.dispose()
        val imgOut = if (maxXPage >= pageWidth - 1) img else {
          val imgShift = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_ARGB)
          val g = imgShift.createGraphics()
          g.setColor(Color.white)
          g.fillRect(0, 0, pageWidth, pageHeight)
          g.drawImage(img, (pageWidth - maxXPage)/2, 0, null)
          g.dispose()
          img.flush()
          imgShift
        }
        val pageF = groupDir / s"notebook-p$pageIdx.png"
        ImageIO.write(imgOut, "png", pageF)
        imgOut.flush()

        loopPage(rem = rem1, pageIdx = pageIdx + 1)
      }

      loopPage(wordIndices, pageIdx = 1)
    }
  }
}
