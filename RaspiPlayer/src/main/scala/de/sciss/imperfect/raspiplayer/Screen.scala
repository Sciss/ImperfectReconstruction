/*
 *  Screen.scala
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

package de.sciss.imperfect.raspiplayer

import de.sciss.kollflitz.Vec

object Screen {
  def apply(index: Int): Screen = seq(index)

  final val seq: Vec[Screen] = Vector(
    Screen(index = 0 /* 11 */, isHanging = true , orientations = List(0)),
    Screen(index = 1 /* 12 */, isHanging = true , orientations = List(0)),
    Screen(index = 2 /* 13 */, isHanging = false, orientations = List(0, 90, 180)),
    Screen(index = 3 /* 14 */, isHanging = true , orientations = List(0)),
    Screen(index = 4 /* 15 */, isHanging = false, orientations = List(0, 180, 270)),
    Screen(index = 5 /* 16 */, isHanging = false, orientations = List(0)),
    Screen(index = 6 /* 17 */, isHanging = true , orientations = List(0)),
    Screen(index = 7 /* 18 */, isHanging = false, orientations = List(0, 180, 270))
  )
}

/** @param index        zero-based index, so zero is the first channel with IP ending in .11
  * @param isHanging    if true, a hanging screen, if false, a screen on the floor
  * @param orientations allowed viewing orientations for "normal" images, in clockwise degrees
  */
final case class Screen(index: Int, isHanging: Boolean, orientations: List[Int])