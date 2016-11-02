/*
 *  Test.scala
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

package de.sciss.imperfect.trajectory

import java.io.FileInputStream

import de.sciss.file._
import de.sciss.serial.{DataInput, DataOutput}

object Test {
  // note: needs `-Xmx4g`
  def main(args: Array[String]): Unit = {
    val fIn     = userHome / "Documents" / "projects" / "Imperfect" / "cern_daten" / "CERN_trajectories.txt"
    val fOut    = fIn.replaceExt("bin")
    val t1      = System.currentTimeMillis()
    val events  = if (fOut.isFile) {
//       XXX TODO --- this is too slow - needs to be buffered
       val dIn = DataInput.open(fIn)
//      val fis = new FileInputStream(fIn)
      try {
//        val arr = new Array[Byte](fis.available())
//        fis.read(arr)
//        val dIn = DataInput(arr)
        Events.read(dIn)
      } finally {
        dIn.close()
//        fis.close()
      }
    } else {
      val res   = Events.parse(io.Source.fromFile(fIn))   // 80260 events in 282 sec
      val dOut  = DataOutput.open(fOut)
      try {
        Events.write(res, dOut)
      } finally {
        dOut.close()
      }
      res
    }
    val t2 = System.currentTimeMillis()
    println(s"Got ${events.size} events (took ${t2-t1}ms).")
  }
}
