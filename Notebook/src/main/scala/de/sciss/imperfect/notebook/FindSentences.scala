/*
 *  FindSentences.scala
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

import de.sciss.file._
import de.sciss.kollflitz
import de.sciss.kollflitz.Vec

import scala.xml.XML

object FindSentences {
  def main(args: Array[String]): Unit = run(print = true)

  def run(print: Boolean): Vec[Vec[Vec[(String, Int)]]] = {
    val words = (40 to 43).flatMap { page =>
      val svgIn     = userHome / "Documents"/"projects"/"Imperfect"/"scans"/"notebook2016"/"notebook2016_p%dm-annot.svg".format(page)
      val xmlRoot   = XML.load(svgIn.toURI.toURL)
      val pathsXML  = xmlRoot.\\("path")
      val (_, pathsWordXML) = pathsXML.partition(_.\("@style").text.contains("stroke:#0000ff"))
      val wordsPage = pathsWordXML.map(n => (n\"@notebook").text.trim).toVector
      wordsPage
    } .zipWithIndex

    import kollflitz.Ops._
    val endings   = Seq(".", "?", "!", ";", ":", ".\"", "?\"", "!\"", "a? -erase")
    val sentences = words.groupWith { case ((w, i), _) => !endings.exists(w.endsWith) } .toVector

    if (print) sentences.zipWithIndex.foreach { case (s, i) =>
      println(s.mkString(f"$i%02d: ", " ", ""))
    }

    val numChannels = 8
    val groups = Vector.tabulate(numChannels)(ch => sentences.zipWithIndex.filter(_._2 % numChannels == ch).map(_._1))
    if (print) {
      println("Words:")
      groups.zipWithIndex.foreach { case (s, i) =>
        val words = s.flatten
        println(f"  $i%02d: ${words.size}")
      }
      println("Chars:")
      groups.zipWithIndex.foreach { case (s, i) =>
        val words = s.flatten
        val chars = words.mkString
        println(f"  $i%02d: ${chars.length}")
      }
    }
    // println(s"Groups: ${groups.flatten.map(_.size)} words, ${groups.flatten.flatten.map(_.length)} chars.")

    groups
  }
}
