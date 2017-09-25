/*
 *  Config.scala
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

package de.sciss.imperfect.raspikeys

object Config {
  val controlIP = "192.168.0.11"
}
final case class Config(isTest: Boolean = false, keyShutdown : Char = '1', keyReboot: Char = '3',
                        buttonShutdown: Int = 0, buttonReboot: Int = 0) {

  val hasButtons: Boolean = buttonShutdown != 0 || buttonReboot != 0
}
