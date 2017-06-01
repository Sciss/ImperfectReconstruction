/*
 *  PlayerFrame.scala
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

package de.sciss.imperfect.cracks

import de.sciss.numbers
import java.awt.image.BufferedImage
import java.awt.{AlphaComposite, Color, Graphics, Graphics2D, GraphicsConfiguration, GraphicsDevice, Point, Toolkit}

import scala.annotation.switch

object PlayerFrame {
  final val TIMER_FPS   : Int = 25
  final val TIMER_PERIOD: Int = 1000 / TIMER_FPS
  final val FADE_FRAMES : Int = TIMER_FPS  * 10
}
final class PlayerFrame(config: Config, screen: GraphicsDevice, screenConf: GraphicsConfiguration)
  extends java.awt.Frame(null: String, screenConf) { w =>

  import PlayerFrame._

  private[this] val imageName   = s"cracks${config.thisChannel + 1}.png"
  private[this] val urlImage    = getClass.getResource(imageName)
  private[this] val image       = if (urlImage == null) null else Toolkit.getDefaultToolkit.getImage(urlImage)

  @volatile
  private[this] var _stageSet   = 0

  private[this] var _stageTaken = 0

  private[this] var _frameSet   = 0
  private[this] var _frameTaken = 0

  private[this] val timer       = new javax.swing.Timer(TIMER_PERIOD, _ => draw())

  if (image == null) println(s"Warning: could not find image resource '$imageName'")

  setUndecorated  (true)
  setIgnoreRepaint(true)
  setBackground(new Color(config.background))

  /** Stages:
    *
    * - 0 init
    * - 1 fade-in
    * - 2 trace
    * - 3 remove trace
    * - 4 fade-out
    */
  def setStage(i: Int): Unit = {
    _stageSet = i
    if (i == 1) {
      timer.restart()
    }
  }

  private[this] var fadeRemain = 0

//  private[this] val NominalWidth  = 1920
//  private[this] val NominalHeight = 1080
  private[this] val VisibleWidth  = 1024 // 1280
  private[this] val VisibleHeight = 1024
  private[this] val OffScreenImg  = new BufferedImage(VisibleWidth, VisibleHeight, BufferedImage.TYPE_INT_ARGB)
  private[this] val OffScreenG    = {
    val res = OffScreenImg.createGraphics()
    res.setColor(Color.black)
    res.fillRect(0, 0, VisibleWidth, VisibleHeight)
    res
  }

  private[this] def draw(): Unit = {
    _frameSet += 1
//    repaint()
//    getToolkit.sync()

    paintOffScreen()
    val width     = w.getWidth
    val height    = w.getHeight
    val strategy  = w.getBufferStrategy
    val x         = (width  - 1024) >> 1
    val y         = (height - 1024) >> 1
    do {
      do {
        val g = strategy.getDrawGraphics
        g.setColor(w.getBackground)
        g.fillRect(0, 0, width, height)
        g.drawImage(OffScreenImg, x, y, null)
        //            0,             0, NominalWidth, VisibleHeight, null)
//        if (width == NominalWidth && height == NominalHeight) {
//          g.drawImage(OffScreenImg,            0,             0, NominalWidth, VisibleHeight,
//            0,             0, NominalWidth, VisibleHeight, null)
//          g.drawImage(OffScreenImg,            0, VisibleHeight, NominalWidth, NominalHeight,
//            NominalWidth,             0, VisibleWidth, VisibleHeight, null)
//        } else {
//          if (!haveWarnedWinSize) {
//            warn(s"Full screen window has dimensions $width x $height instead of $NominalWidth x $NominalHeight")
//            haveWarnedWinSize = true
//          }
//          g.drawImage(OffScreenImg,            0,        0, width,        height/2,
//            0,        0, NominalWidth, VisibleHeight, null)
//          g.drawImage(OffScreenImg,            0, height/2, width,        height,
//            NominalWidth,        0, VisibleWidth, VisibleHeight, null)
//          g.dispose()
//        }
      } while (strategy.contentsRestored())
      strategy.show()
    } while (strategy.contentsLost())
  }

  private def paintOffScreen(): Unit = {

    if (_frameTaken == _frameSet) return
    _frameTaken = _frameSet

    val g2 = OffScreenG // g.asInstanceOf[Graphics2D]

    var stage = _stageTaken
    if (_stageSet != stage) {
      stage       = _stageSet
      _stageTaken = stage
      println(s"New stage $stage")
      (stage: @switch) match {
        case 1 | 4 =>
          fadeRemain = FADE_FRAMES
        case _ =>
      }
    }

    val width   = 1024 // getWidth
    val height  = 1024 // getHeight
    val x         = 0 // (width  - 1024) >> 1
    val y       = 0 // (height - 1024) >> 1
    g2.setColor(w.getBackground)
    g2.fillRect(0, 0, width, height)

    (stage: @switch) match {
      case 0 =>
      case 1 | 4 =>
        val compOrig  = g2.getComposite
        val fd        = fadeRemain
        // println(s"fd $fd FADE_FRAMES $FADE_FRAMES")
        val fd1       = if (stage == 1) FADE_FRAMES - fd else fd
        import numbers.Implicits._
        val alpha     = fd1.toFloat / FADE_FRAMES // fd1.linlin(0, FADE_FRAMES, 1f, 0f)
        // println(s"alpha $alpha")
        if (fd > 1) fadeRemain = fd - 1
        val comp      = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
        g2.setComposite(comp)
        if (image != null) {
          g2.drawImage(image, x, y, this)
        }
        g2.setComposite(compOrig)

      case 2 | 3 =>
        if (image != null) g2.drawImage(image, x, y, this)

      case _ =>
    }

  //        val m = debugMessage
  //        if (!m.isEmpty) {
  //          g.setFont(fnt)
  //          val fm = g.getFontMetrics
  //          val tw = fm.stringWidth(debugMessage)
  //          g.setColor(Color.white)
  //          g.drawString(m, (getWidth - tw)/2, getHeight/2 - fm.getAscent)
  //        }
  }

  def fullScreen(): Unit = {
    w.setSize(screenConf.getBounds.getSize)
    screen.setFullScreenWindow(w)
    w.requestFocus()

    // "hide" cursor
    val cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val cursor = w.getToolkit.createCustomCursor(cursorImg, new Point(0, 0), "blank")
    w.setCursor(cursor)

    // Ok, so there is some weird bug in that sometime the
    // buffer doesn't have the correct size. For now, it
    // seems, waiting with the thread a bit helps.
      Thread.sleep(50)
    w.createBufferStrategy(2)
    Thread.sleep(50)
  }
}