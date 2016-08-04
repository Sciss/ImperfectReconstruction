/*
 *  Exposure.scala
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

package de.sciss.imperfect.difference

import java.util.Locale

import com.hopding.jrpicam.RPiCamera
import com.hopding.jrpicam.enums.{AWB, DRC, Encoding, MeteringMode, Exposure => PiExposure}
import de.sciss.file._
import scopt.OptionParser

import scala.annotation.switch
import scala.util.Try

object Exposure {
  case class Config(useGPIO   : Boolean       = false,
                    outputDir : File          = file("/") / "media" / "pi" / "exposure" / "Pictures",
                    width     : Int           = 3280,
                    height    : Int           = 2464, // /2
                    delayTime : Int           = 10000,
                    shutdown  : Boolean       = false,
                    shutter   : Int           = 10000,
                    iso       : Int           = 100,
                    exposure  : PiExposure    = PiExposure.AUTO,
                    awb       : AWB           = AWB.AUTO,
                    redGain   : Double        = 1.0,
                    blueGain  : Double        = 1.0,
                    drc       : DRC           = DRC.OFF,
                    metering  : MeteringMode  = MeteringMode.AVERAGE,
                    flipH     : Boolean       = false,
                    flipV     : Boolean       = false,
                    encoding  : Encoding      = Encoding.JPG
                   )

  sealed trait State
  case object StatePause    extends State
  case object StateRecord   extends State
  case object StateShutdown extends State

  @volatile
  private[this] var state: State = StatePause

  def main(args: Array[String]): Unit = {
    val parser = new OptionParser[Config]("Exposure") {
      opt[File  ]('d', "output-dir")   text "Output directory" action { (x, c) => c.copy(outputDir = x) }
      opt[Int   ]('t', "delay-time")   text "Delay time between photos in milliseconds" action { (x, c) => c.copy(delayTime = x) }
      opt[Int   ]('w', "width")        text "Image width in pixels"  action { (x, c) => c.copy(width  = x) }
      opt[Int   ]('h', "height")       text "Image height in pixels" action { (x, c) => c.copy(height = x) }
//      opt[Double]("min-phrase")        text "Minimum phrase duration in seconds" action { (x, c) => c.copy(minPhrase = x) }
      opt[Unit  ]('g', "gpio")         text "Enable GPIO equipment (key matrix and LED)" action { (_, c) => c.copy(useGPIO = true) }
      opt[Unit  ]('z', "shutdown")     text "Enable computer shutdown after termination" action { (_, c) => c.copy(shutdown = true) }
      opt[Int   ]('s', "shutter")      text "Shutter speed in microseconds" action { (_, c) => c.copy(shutdown = true) }
      opt[Int   ]('i', "iso")          text "ISO value" action { (x, c) => c.copy(iso = x) }
      opt[String]('e', "exposure")     text "Exposure type (auto, night, nightpreview, backlight, spotlight, sports, snow, beach, verylong, fixedpfs, antishake, fireworks)" action { (x, c) =>
        c.copy(exposure = PiExposure.valueOf(x.toUpperCase(Locale.US)))
      }
      opt[String]('a', "awb")          text "Automatic white balance (off, auto, sun, cloud, shade, tungsten, fluorescent, incandescent, flash, horizon)" action { (x, c) =>
        c.copy(awb = AWB.valueOf(x.toUpperCase(Locale.US)))
      }
      opt[String]('c', "drc")          text "Dynamic range compression (off, low, medium, high)" action { (x, c) =>
        c.copy(drc = DRC.valueOf(x.toUpperCase(Locale.US)))
      }
      opt[String]('m', "metering")     text "Metering mode (average, spot, backlit, matrix)" action { (x, c) =>
        c.copy(metering = MeteringMode.valueOf(x.toUpperCase(Locale.US)))
      }
      opt[String]('f', "encoding")     text "Encoding (jpg, bmp, gif, png)" action { (x, c) =>
        c.copy(encoding = Encoding.valueOf(x.toUpperCase(Locale.US)))
      }
      opt[Unit  ]('x', "flip-h")       text "Flip horizontally" action { (_, c) => c.copy(flipH = true) }
      opt[Unit  ]('y', "flip-v")       text "Flip vertically"  action { (_, c) => c.copy(flipV = true) }
      opt[Double]("red")               text "Red gain"  action { (x, c) => c.copy(redGain  = x) }
      opt[Double]("blue")              text "Blue gain" action { (x, c) => c.copy(blueGain = x) }
    }
    parser.parse(args, Config()).fold(sys.exit(1))(run)
  }

  def run(config: Config): Unit = {
    import config._
    require(outputDir.isDirectory && outputDir.canWrite, s"Cannot write to $outputDir")

    val countSite = outputDir.children(_.name.startsWith("site-")).flatMap { f =>
      val n = f.name
      Try(n.substring(5).toInt).toOption
    } .sorted.lastOption.getOrElse(0) + 1

    val siteDir = outputDir / s"site-$countSite"
    require(siteDir.mkdir())
    println(s"Next site will be #$countSite")

    val keys  = if (useGPIO) new KeyMatrix    else null
    val led   = if (useGPIO) new DualColorLED else null

    val cam = new RPiCamera(siteDir.path)
    // cf. https://raspberrypi.stackexchange.com/questions/14047/
    cam.setShutter(shutter) // 500000
    cam.setISO(iso)
    cam.setExposure(exposure)
    cam.setAWB(awb)
    cam.setAWBGains(redGain, blueGain)
    cam.setMeteringMode(metering)
    if (flipH) cam.setHorizontalFlipOn()
    if (flipV) cam.setVerticalFlipOn()
    cam.setDRC(drc)
    cam.setEncoding(encoding)
    cam.turnOffPreview()
    // cam.setTimeout()
    val ext = encoding.toString

    if (useGPIO) led.pulseGreen()  // 'ready'

    //    // XXX TODO --- this could be slow for lots of pictures; perhaps use 'jumping'
    //    var count = outputDir.children(_.name.startsWith("frame-")).flatMap { f =>
    //      val n = f.name
    //      Try(n.substring(6, n.indexOf('.', 6)).toInt).toOption
    //    } .sorted.lastOption.getOrElse(0) + 1
    var count = 1

    //    println(s"Next frame will be #$count")

    while (state != StateShutdown) {
      if (state == StateRecord) {
        val name = s"frame-$count.$ext"
        cam.takeStill(name, width, height)
        count += 1
      }
      var dlyRemain = delayTime
      while (dlyRemain > 0) {
        Thread.sleep(100)
        if (useGPIO) (keys.read(): @switch) match {
          case '1' =>
            if (state != StateRecord) {
              state     = StateRecord
              dlyRemain = 0
              led.pulseRed()
            }
          case '2' =>
            if (state != StatePause) {
              state = StatePause
              led.pulseGreen()
            }
          case '9' =>
            state     = StateShutdown
            dlyRemain = 0
            led.blinkRed()

          case _ =>
        }
        dlyRemain -= 100
      }
    }

    import scala.sys.process._
    if (shutdown) Seq("sudo", "shutdown", "now").! else sys.exit()
  }
}