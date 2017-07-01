package de.sciss.imperfect.raspiplayer

import scala.util.Random

object TrunksTest {
  def main(args: Array[String]): Unit = {
    implicit val screens: Screens = Screens.xCoAx
    implicit val rnd: Random = Random.self
    val set = VideoSetTrunks.select()
    set.foreach(println)
  }
}
