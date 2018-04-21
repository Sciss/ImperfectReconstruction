/*
 *  DualColorLED.scala
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

import com.pi4j.io.gpio.{GpioFactory, PinState, RaspiPin}

final class DualColorLED {
  private[this] val io    = GpioFactory.getInstance
  private[this] val pinR  = io.provisionDigitalOutputPin(RaspiPin.GPIO_00, "Red"  , PinState.LOW)
  private[this] val pinG  = io.provisionDigitalOutputPin(RaspiPin.GPIO_01, "Green", PinState.LOW)
  pinR.setShutdownOptions(true, PinState.LOW)
  pinG.setShutdownOptions(true, PinState.LOW)

  def red(): Unit = {
    pinG.low()
    pinR.high()
  }

  def pulseRed(): Unit = {
    pinG.low()
    pinR.pulse(1000)
  }

  def blinkRed(): Unit = {
    pinG.low()
    pinR.blink(167, 1000)
  }

  def green(): Unit = {
    pinR.low()
    pinG.high()
  }

  def pulseGreen(): Unit = {
    pinR.low()
    pinG.pulse(1000)
  }

  def blinkGreen(): Unit = {
    pinR.low()
    pinG.blink(167, 1000)
  }

  def off(): Unit = {
    pinR.low()
    pinG.low()
  }
}