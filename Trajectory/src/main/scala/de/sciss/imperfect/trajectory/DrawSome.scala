/*
 *  DrawSome.scala
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

package de.sciss.imperfect.trajectory

import java.awt.{Color, RenderingHints}
import javax.swing.Timer

import scala.swing.{Component, Dimension, Graphics2D, MainFrame, Swing}
import de.sciss.numbers.Implicits._

object DrawSome {
  def main(args: Array[String]): Unit = {
    val events = Events.readStd(max = 100)
    Swing.onEDT(run(events))
  }

  def run(events: Array[Event]): Unit = {
    val w = 1440
    val h = w * 540 / 3840
    val c = new Component {
      preferredSize = new Dimension(w, h)
      opaque        = true

      override protected def paintComponent(g: Graphics2D): Unit = {
        val p = peer
        val w = p.getWidth
        val h = p.getHeight
        g.setColor(Color.black)
        g.fillRect(0, 0, w, h)
        g.setColor(Color.white)
//        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON)
//        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE )
        paintEvents(events, g, w = w, h = h)
      }
    }

    new MainFrame {
      contents = c
      pack().centerOnScreen()
      open()
    }

    val t = new Timer(25, Swing.ActionListener { _ =>
      c.repaint()
    })
    t.setRepeats(true)
    t.start()
  }

  private[this] var animCount = 0

  def paintEvents(events: Array[Event], g: Graphics2D, w: Int, h: Int): Unit = {
    val wh = w/2
    val hh = h/2

//    def mapH(i: Double) = (math.sqrt(math.abs(i / 2000)) * i.signum + 1) * wh
//    def mapV(i: Double) = (math.sqrt(math.abs(i /  500)) * i.signum + 1) * hh

    def mapXH(i: Double) = (math.pow(math.abs(i / 2000), 1.0/3.0) * i.signum + 1) * wh
    def mapXV(i: Double) = (math.pow(math.abs(i / 2000), 1.0/3.0) * i.signum + 1) * hh
    def mapYV(i: Double) = (math.pow(math.abs(i /  500), 1.0/3.0) * i.signum + 1) * hh

    def mapZH(i: Double) = i.linlin(-1000, 1000, 0, w)

    val numEvt  = 10
    var i       = animCount % (events.length - numEvt)
    animCount += 1
    val stop  = i + numEvt
    while (i < stop) {
      val ev = events(i)
      val px = ev.particles
      var j = 0
      while (j < px.length) {
        val p = px(j)
        var k = 1
        val pt = p.traj
        val y2 = (j - 6) * 50
        if (k < pt.length) {
          val p0 = pt(0)
//          var x0 = mapXH(p0.x)
          var x0 = mapZH(p0.z)
//          var y0 = mapYV(p0.y)
          var y0 = mapXV(p0.x)
          while (k < pt.length) {
            val p1 = pt(k)
//            val x1 = mapXH(p1.x)
            val x1 = mapZH(p1.z)
//            val y1 = mapYV(p1.y)
            val y1 = mapXV(p1.x)
//            g.drawLine(x0.toInt, y0.toInt, x1.toInt, y1.toInt)
            g.drawLine(x0.toInt, y0.toInt + y2, x1.toInt, y1.toInt + y2)
            x0 = x1
            y0 = y1
            k += 1
          }
        }
        j += 1
      }
      i +=1
    }
  }
}
