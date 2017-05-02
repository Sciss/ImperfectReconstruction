package de.sciss.imperfect.difference

import de.sciss.file._
import de.sciss.numbers.Implicits._

object CoverSelect {
  def main(args: Array[String]): Unit = {
    val baseDir   = file("/") /"data" / "projects" / "Imperfect"
    val coverDir  = baseDir / "catalog" / "cover"
    val dirIn     = coverDir / "site-2out_catalog"
    val dirOut    = coverDir / "site-2out_catalogSel"
    import MakeCatalogCover.fileNameOrdering
    val filesIn   = dirIn.children(_.ext == "jpg").sorted
    val numIn     = filesIn.size
    println(s"num-in = $numIn")
    val numOut    = 300
    require(numOut <= numIn)
    if (!dirOut.exists()) dirOut.mkdir()
    if (dirOut.children(_.ext == "jpg").nonEmpty) {
      println(s"Output directory $dirOut is not empty. Aborting.")
      sys.exit(1)
    }

    for (j <- 1 to numOut) {
//      val j = (i + 1).linlin(1, numIn, 1, numOut).round.toInt
      val i     = j.linlin(1, numOut, 1, numIn).round - 1
      val fIn   = filesIn(i)
      val fOut  = dirOut / s"frame-$j.jpg"
      import sys.process._
      val cmd   = Seq("ln", "-s", fIn.path, fOut.path)
      cmd.!!
      // println(cmd.mkString(" "))
    }
  }
}
