/*
 *  KeyMatrix.scala
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

import com.pi4j.io.gpio.{GpioFactory, PinMode, PinPullResistance, PinState}

import scala.util.control.NonFatal

// XXX TODO -- allow config, different matrix sizes etc.
object KeyMatrix {
  val NotPressed = 'X'

  val KeyPad = Array(
    Array('1', '2', '3'),
    Array('4', '5', '6'),
    Array('7', '8', '9')
  )

  import com.pi4j.io.gpio.RaspiPin._

  /*

    the wires on the key pad are as follows:
    green, blue, purple = Y1, Y2, Y3
    grey, white, black, brown = X1, X2, X3, X4

    thus

    green : GPIO 4
    blue  : GPIO 27
    purple: GPIO 22

    grey  : GPIO 23
    white : GPIO 24
    black : GPIO 25

    brown : not used

   */

  val rows    = Array(GPIO_07, GPIO_02, GPIO_03)  // BCM: 4, 27, 22
  val columns = Array(GPIO_04, GPIO_05, GPIO_06)  // BCM: 23, 24, 25

  def test(): Unit = {
    val m   = new KeyMatrix
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
        val m   = new KeyMatrix
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
final class KeyMatrix {
  import KeyMatrix._

  // so we use GPIO_00 and GPIO_01 for the LED (could drop one)

  private[this] val io      = GpioFactory.getInstance

  private[this] val rowPins = rows   .map(pin => io.provisionDigitalMultipurposePin(pin, PinMode.DIGITAL_INPUT, PinPullResistance.PULL_UP))
  private[this] val colPins = columns.map(pin => io.provisionDigitalMultipurposePin(pin, PinMode.DIGITAL_OUTPUT))

  // Reinitialize all rows and columns as input at exit
  (rowPins ++ colPins).foreach(_.setShutdownOptions(true, PinState.LOW, PinPullResistance.PULL_UP, PinMode.DIGITAL_INPUT))

  /** Retrieves the currently pressed key, or `NotPressed` in case no key press is detected. */
  def read(): Char = {
    // Set all columns as output low
    colPins.foreach { pin =>
      pin.setMode(PinMode.DIGITAL_OUTPUT)
      pin.low()
    }

    // Set all rows as input
    rowPins.foreach { pin =>
      pin.setMode(PinMode.DIGITAL_INPUT)
      pin.setPullResistance(PinPullResistance.PULL_UP)
    }

    // Scan rows for pushed key/button
    // A valid key press should set "rowVal"  between 0 and 3.
    val rowIdx = rowPins.indexWhere(_.isLow)

    // if rowIdx is not in 0 to 2 then no button was pressed and we can exit
    if (rowIdx < 0) return NotPressed

    // Convert columns to input
    colPins.foreach { pin =>
      pin.setMode(PinMode.DIGITAL_INPUT)
      pin.setPullResistance(PinPullResistance.PULL_DOWN)
    }

    // Switch the i-th row found from scan to output
    val rowPin = rowPins(rowIdx)
    rowPin.setMode(PinMode.DIGITAL_OUTPUT)
    rowPin.high()

    // Scan columns for still-pushed key/button
    // A valid key press should set "colIdx"  between 0 and 2.
    val colIdx = colPins.indexWhere(_.isHigh)

    // if colIdx is not in 0 to 2 then no button was pressed and we can exit
    if (colIdx < 0) return NotPressed

    KeyPad(rowIdx)(colIdx)
  }
}