package de.sciss.imperfect.trajectory

import de.sciss.file._

object Test {
  def main(args: Array[String]): Unit = {
    val p = userHome / "Documents" / "projects" / "Imperfect" / "cern_daten" / "CERN_trajectories.txt"
    val events = Event.parse(io.Source.fromFile(p))
    println(s"Got ${events.size} event.s")
  }
}
