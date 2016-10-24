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

import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import scala.annotation.elidable
import scala.annotation.elidable._

package object raspiplayer {
  var showLog = false

  private[this] val logHeader = new SimpleDateFormat("[d MMM yyyy, HH:mm''ss.SSS] 'imperfect' - ", Locale.US)

  @elidable(CONFIG) def log(what: => String): Unit =
    if (showLog) Console.out.println(s"${logHeader.format(new Date())}$what")

  val ServerPort    = 57110
  val ServerAddress = new InetSocketAddress("192.168.0.11", ServerPort)
}
