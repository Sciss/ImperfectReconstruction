/*
 *  package.scala
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

package de.sciss.imperfect

import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import de.sciss.kollflitz.Vec

import scala.annotation.elidable
import scala.annotation.elidable._

package object cracks {
  var showLog = true

  private[this] val logHeader = new SimpleDateFormat("[d MMM yyyy, HH:mm''ss.SSS] 'imperfect' - ", Locale.US)

  @elidable(CONFIG) def log(what: => String): Unit =
    if (showLog) Console.out.println(s"${logHeader.format(new Date())}$what")

  val ServerPort    = 57110
  val ServerAddress = new InetSocketAddress("192.168.0.11", ServerPort)

  implicit final class MyIntOps(private val i: Int) extends AnyVal {
    def minutes(sec: Int): Int = i * 60 + sec
  }

  final val IndicesIn: Vec[Int] = 0 until 8
  final val OrientIn : Vec[Int] = Vector(0, 90, 180, 270)
}
