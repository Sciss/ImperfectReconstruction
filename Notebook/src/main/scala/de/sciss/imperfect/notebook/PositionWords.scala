/*
 *  PositionWords.scala
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
import de.sciss.fscape.Graph
import de.sciss.fscape.graph
import de.sciss.fscape.stream.Control
import scopt.OptionParser

object PositionWords {
  case class Config(inTemp   : File     = file("in-%.png"),
                    outTemp  : File     = file("out-%.png"),
                    overwrite: Boolean  = false
                   )

  def main(args: Array[String]): Unit = {
    val parser = new OptionParser[Config]("PositionWords") {
      opt[File  ]('i', "input-template")  required() text "Template in file" action { (x, c) => c.copy(inTemp  = x) }
      opt[File  ]('d', "output-template") required() text "Output template"  action { (x, c) => c.copy(outTemp = x) }
      opt[Unit  ]("overwrite") text "Overwrite existing files"  action { (_, c) => c.copy(overwrite = true) }
    }
    parser.parse(args, Config()).fold(sys.exit(1))(run)
  }

  def run(config: Config): Unit = {
    import config._
    val g = Graph {
      import graph._
      val fIn0  = formatFile(inTemp , 1)
      val fOut0 = formatFile(outTemp, 1)
      val in    = ImageFileIn(fIn0, numChannels = 4)
      val sig   = in
      ImageFileOut(fOut0, ImageFile.Spec(width = 766, height = 1059, numChannels = 4), in = sig)
    }
    val cc  = Control.Config()
    val ctl = Control(cc)
    ctl.run(g)

    import scala.concurrent.ExecutionContext.Implicits.global
    ctl.status.onComplete {
      x => println(s"Result: $x")
    }
  }
}
