/*
 *  Buttons.scala
 *  (Imperfect Reconstruction)
 *
 *  Copyright (c) 2016-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v2+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.imperfect.difference

import com.pi4j.io.gpio.{GpioFactory, PinMode, PinPullResistance}

import scala.util.control.NonFatal

object Buttons {
  val NotPressed = 'X'

  import com.pi4j.io.gpio.RaspiPin._

  /*

    the wires are as follows:
    brown  = GND-jacket / +3V
    red    = black button / header 15
    orange = red button   / header 16

    or with the red/green button box:

    brown = GND --> +3V / header 17 (9th from top-left)
    yellow = green button / header 15 (8th from top-left)
    orange = red button / header 16 (8th from top-right)

   */

  private val rows = Array(GPIO_04, GPIO_05)  // header 16, 18

  def test(): Unit = {
    val m   = new Buttons
    var old = NotPressed
    while (true) {
      val c = m.read()
      if (c != NotPressed && c != old) {
        println(s"Pressed: $c")
      }
      old = c
      Thread.sleep(100)
    }
  }

  def run()(fun: Char => Unit): Unit = {
    val t = new Thread {
      override def run(): Unit = {
        val m   = new Buttons
        var old = NotPressed
        while (true) {
          val c = m.read()
          if (c != NotPressed && c != old) {
            try {
              fun(c)
            } catch {
              case NonFatal(ex) =>
                ex.printStackTrace()
            }
          }
          old = c
          Thread.sleep(100)
        }
      }
    }
    //    t.setDaemon(true)
    t.start()
  }
}
final class Buttons {
  import Buttons._

  private[this] val io = GpioFactory.getInstance

  private[this] val rowPins = rows.map(pin =>
    io.provisionDigitalMultipurposePin(pin, PinMode.DIGITAL_INPUT, PinPullResistance.PULL_DOWN))

  /** Retrieves the currently pressed key, or `NotPressed` in case no key press is detected. */
  def read(): Char = {
    val is1 = rowPins(0).isHigh
    val is2 = rowPins(1).isHigh
    if (is1 && is2) '9'
    else if (is1)   '1'
    else if (is2)   '2'
    else NotPressed
  }
}