/*
 *  Config.scala
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

import de.sciss.file._

final case class Config(isControl: Boolean = false, clientPort: Int = 57120,
                        testVideo: File = userHome / "Videos" / "test.mp4", dumpOSC: Boolean = false)