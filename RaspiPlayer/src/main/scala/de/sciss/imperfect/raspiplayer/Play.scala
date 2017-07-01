/*
 *  Play.scala
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

import de.sciss.osc

object Play {
//  def unapply(m: osc.Message): Option[(String, Float, Float, Int, Float, Float)] = m match {
//    case osc.Message("/play", file: String, start: Float, duration: Float, orientation: Int,
//                              fadeIn: Float, fadeOut: Float) =>
//      Some((file, start, duration, orientation, fadeIn, fadeOut))
//
//    case _ => None
//  }

//  /** OSC command for a client to play a video segment.
//    *
//    * @param file         name of video
//    * @param start        start time offset in seconds
//    * @param duration     duration in seconds
//    * @param orientation  orientation: 0, 90, 180, 270
//    * @param fadeIn       fade in duration in seconds (or zero for no fade)
//    * @param fadeOut      fade out duration in seconds (or zero for no fade)
//    */
//  def apply(file: String, start: Float, duration: Float, orientation: Int,
//            fadeIn: Float, fadeOut: Float): osc.Message =
//    osc.Message("/play", file, start, duration, orientation, fadeIn, fadeOut)
}
/** OSC command for a client to play a video segment.
  *
  * @param file         name of video
  * @param delay        optional (black) delay in seconds
  * @param start        start time offset in seconds
  * @param duration     duration in seconds
  * @param orientation  orientation: 0, 90, 180, 270
  * @param fadeIn       fade in duration in seconds (or zero for no fade)
  * @param fadeOut      fade out duration in seconds (or zero for no fade)
  */
final case class Play(file: String, delay: Float, start: Float, duration: Float, orientation: Int,
                      fadeIn: Float, fadeOut: Float)
  extends osc.Message("/play", file, delay, start, duration, orientation, fadeIn, fadeOut) {

  override def toString =
    s"Play($file, delay = $delay, start = $start, duration = $duration, orientation = $orientation, fadeIn = $fadeIn, fadeOut = $fadeOut"
}

object PlayMessage {
  def unapply(m: osc.Message): Option[Play] = m match {
    case osc.Message("/play", file: String, delay: Float, start: Float, duration: Float, orientation: Int,
    fadeIn: Float, fadeOut: Float) =>
      Some(Play(file = file, delay = delay, start = start, duration = duration, orientation = orientation,
        fadeIn = fadeIn, fadeOut = fadeOut))
    case _ => None
  }
}