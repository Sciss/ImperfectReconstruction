/*
 *  Screen.scala
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

package de.sciss.imperfect.raspiplayer

import de.sciss.kollflitz.Vec
import de.sciss.numbers

/** @param isHanging    if true, a hanging screen, if false, a screen on the floor
  * @param orientations allowed viewing orientations for "normal" images, in clockwise degrees
  */
final case class Screen(/* index: Int, */ isHanging: Boolean, orientations: List[Int])

object Screens {
  final val escSeq: Vec[Screen] = Vector(
    Screen(/* index = 0, */ isHanging = true , orientations = List(0)),
    Screen(/* index = 1, */ isHanging = true , orientations = List(0)),
    Screen(/* index = 2, */ isHanging = false, orientations = List(0, 90, 180)),
    Screen(/* index = 3, */ isHanging = true , orientations = List(0)),
    Screen(/* index = 4, */ isHanging = false, orientations = List(0, 180, 270)),
    Screen(/* index = 5, */ isHanging = false, orientations = List(0)),
    Screen(/* index = 6, */ isHanging = true , orientations = List(0)),
    Screen(/* index = 7, */ isHanging = false, orientations = List(0, 180, 270))
  )

  final val xCoAx_screen: Screen = Screen(isHanging = false, orientations = List(0))

  object esc extends Screens {
    def apply(index: Int): Screen = {
      import numbers.Implicits._
      val idxW = index.wrap(0, escSeq.size - 1)
      escSeq(idxW)
    }
  }

  object xCoAx extends Screens {
    def apply(index: Int): Screen = xCoAx_screen
  }
}
trait Screens {
  def apply(index: Int): Screen
}