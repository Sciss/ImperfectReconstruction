/*
 *  package.scala
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

package de.sciss.imperfect

import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import scala.annotation.elidable
import scala.annotation.elidable.CONFIG

package object mesh {
  var showLog = true

  private[this] val logHeader = new SimpleDateFormat("[d MMM yyyy, HH:mm''ss.SSS] 'imperfect' - ", Locale.US)

  final val NominalWidth  = 1920
  final val NominalHeight = 1080
  final val VisibleWidth  = 3840
  final val VisibleHeight =  540
  final val OffScreenImg  = new BufferedImage(VisibleWidth, VisibleHeight, BufferedImage.TYPE_BYTE_GRAY)
  final val OffScreenG    = OffScreenImg.createGraphics()

  @elidable(CONFIG) def log(what: => String): Unit =
    if (showLog) Console.out.println(s"${logHeader.format(new Date())}$what")

  def warn(s: String): Unit =
    Console.err.println(s"Warning: $s")
}
