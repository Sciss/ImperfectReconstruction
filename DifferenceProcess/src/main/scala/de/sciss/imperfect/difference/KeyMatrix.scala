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

package de.sciss.imperfect.difference

import com.pi4j.io.gpio.{GpioFactory, PinMode, PinPullResistance, PinState}

// XXX TODO -- allow config, different matrix sizes etc.
object KeyMatrix {
  val NotPressed = 'X'

  val KeyPad = Array(
    Array('1', '2', '3'),
    Array('4', '5', '6'),
    Array('7', '8', '9')
  )

  import com.pi4j.io.gpio.RaspiPin._

  val rows    = Array(GPIO_07, GPIO_02, GPIO_03)  // 18,23,24,25
  val columns = Array(GPIO_04, GPIO_05, GPIO_06)  // 4,17,22
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