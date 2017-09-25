/*
 *  Buttons.scala
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

import com.pi4j.io.gpio.{GpioFactory, PinMode}

import scala.util.control.NonFatal

object Buttons {
  val NotPressed = 'X'

  private val ButtonPad = Array('1', '2')

  import com.pi4j.io.gpio.RaspiPin._

  /*

    the wires are as follows:
    brown  = GND-jacket / +3V
    red    = black button / GPIO 17
    orange = red button   / GPIO 18

   */

  private val rows = Array(GPIO_00, GPIO_01)  // BCM: 17, 18

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
    io.provisionDigitalMultipurposePin(pin, PinMode.DIGITAL_INPUT)) // , PinPullResistance.PULL_UP))

  /** Retrieves the currently pressed key, or `NotPressed` in case no key press is detected. */
  def read(): Char = {
    // Scan rows for pushed key/button
    // A valid key press should set "rowVal"  between 0 and 3.
    val rowIdx = rowPins.indexWhere(_.isHigh)

    // if rowIdx is not in 0 to 2 then no button was pressed and we can exit
    if (rowIdx < 0) return NotPressed

    ButtonPad(rowIdx)
  }
}