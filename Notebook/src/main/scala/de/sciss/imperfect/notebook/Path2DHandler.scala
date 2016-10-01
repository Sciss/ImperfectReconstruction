/*
 *  Path2DHandler.scala
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

package de.sciss.imperfect.notebook

import java.awt.geom.Path2D

import org.apache.batik.parser.PathHandler

object Path2DHandler {
  final val USE_LOG = false

  def log(what: => String): Unit = if (USE_LOG) println(what)
}
class Path2DHandler extends PathHandler {
  import Path2DHandler._

  private val out     = new Path2D.Double()

  private var x       = 0f
  private var y       = 0f
  private var cx      = 0f
  private var cy      = 0f

  var indent          = "    p."
  var eol             = ";\n"

  private var ended   = false

  private var isFirst = false
  private var startX  = 0f
  private var startY  = 0f

  def result(): Path2D = {
    require(ended, "Path has not yet ended")
    out
  }

  def startPath(): Unit = {
    log("startPath()")
    // newPath()
    x   = 0
    y   = 0
    cx  = 0
    cy  = 0
    isFirst = true
  }

  //    private def newPath(): Unit = {
  //      gpIdx += 1
  //      path   = s"p$gpIdx"
  //      // out.write(s"val $path = new GeneralPath(Path2D.WIND_EVEN_ODD)\n")
  //    }

  def endPath(): Unit = {
    log("endPath()")
    require(!ended, "Path has already ended")
    // out.write(s"$path.closePath()")
    ended = true
  }

  def movetoRel(x3: Float, y3: Float): Unit = {
    log(s"movetoRel($x3, $y3)")
    x  += x3
    y  += y3
    cx  = cx
    cy  = cy
    pathMoveTo(x, y)
  }

  def movetoAbs(x3: Float, y3: Float): Unit = {
    log(s"movetoAbs($x3, $y3)")
    cx  = x3
    cy  = y3
    x   = x3
    y   = y3
    pathMoveTo(x, y)
  }

  def closePath(): Unit = {
    log("closePath()")
    //      x   = 0
    //      y   = 0
    //      cx  = 0
    //      cy  = 0
    linetoAbs(startX, startY)

    //      x   = startX
    //      y   = startY
    //      cx  = startX
    //      cy  = startY
    // out.write(s"${indent}closePath();\n")
    isFirst = true

    //      out.write(s"g2.fill($path)\n")
    //      newPath()
  }

  def linetoRel(x3: Float, y3: Float): Unit = {
    log(s"linetoRel($x3, $y3)")
    x += x3
    y += y3
    cx = x
    cy = y
    pathLineTo(x, y)
  }

  def linetoAbs(x3: Float, y3: Float): Unit = {
    log(s"linetoAbs($x3, $y3)")
    x   = x3
    y   = y3
    cx  = x
    cy  = y
    pathLineTo(x, y)
  }

  def linetoHorizontalRel(x3: Float): Unit = {
    log(s"linetoHorizontalRel($x3)")
    x += x3
    cx = x
    cy = y
    pathLineTo(x, y)
  }

  def linetoHorizontalAbs(x3: Float): Unit = {
    log(s"linetoHorizontalAbs($x3)")
    x  = x3
    cx = x
    cy = y
    pathLineTo(x, y)
  }

  def linetoVerticalRel(y3: Float): Unit = {
    log(s"linetoVerticalRel($y3)")
    cx = x
    y += y3
    cy = y
    pathLineTo(x, y)
  }

  def linetoVerticalAbs(y3: Float): Unit = {
    log(s"linetoVerticalAbs($y3)")
    cx = x
    y  = y3
    cy = y
    pathLineTo(x, y)
  }

  def curvetoCubicRel(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Unit = {
    log(s"curvetoCubicRel($x1, $y1, $x2, $y2, $x3, $y3)")
    val x0  = x + x1
    val y0  = y + y1
    cx      = x + x2
    cy      = y + y2
    x      += x3
    y      += y3
    pathCurveTo(x0, y0, cx, cy, x, y)
  }

  def curvetoCubicAbs(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Unit = {
    log(s"curvetoCubicAbs($x1, $y1, $x2, $y2, $x3, $y3)")
    cx = x2
    cy = y2
    x  = x3
    y  = y3
    pathCurveTo(x1, y1, cx, cy, x, y)
  }

  def curvetoCubicSmoothRel(x2: Float, y2: Float, x3: Float, y3: Float): Unit = {
    log(s"curvetoCubicSmoothRel($x2, $y2, $x3, $y3)")
    val x1 = x * 2 - cx
    val y1 = y * 2 - cy
    cx     = x + x2
    cy     = y + y2
    x     += x3
    y     += y3
    pathCurveTo(x1, y1, cx, cy, x, y)
  }

  def curvetoCubicSmoothAbs(x2: Float, y2: Float, x3: Float, y3: Float): Unit = {
    log(s"curvetoCubicSmoothAbs($x2, $y2, $x3, $y3)")
    val x1  = x * 2 - cx
    val y1  = y * 2 - cy
    cx      = x2
    cy      = y2
    x       = x3
    y       = y3
    pathCurveTo(x1, y1, cx, cy, x, y)
  }

  def curvetoQuadraticRel(p1: Float, p2: Float, p3: Float, p4: Float): Unit = {
    log(s"curvetoQuadraticRel($p1, $p2, $p3, $p4)")
    ???
  }

  def curvetoQuadraticAbs(p1: Float, p2: Float, p3: Float, p4: Float): Unit = {
    log(s"curvetoQuadraticAbs($p1, $p2, $p3, $p4)")
    ???
  }

  def curvetoQuadraticSmoothRel(p1: Float, p2: Float): Unit = {
    log(s"curvetoQuadraticSmoothRel($p1, $p2)")
    ???
  }

  def curvetoQuadraticSmoothAbs(p1: Float, p2: Float): Unit = {
    log(s"curvetoQuadraticSmoothAbs($p1, $p2)")
    ???
  }

  def arcRel(p1: Float, p2: Float, p3: Float, p4: Boolean, p5: Boolean, p6: Float, p7: Float): Unit = {
    log(s"arcRel($p1, $p2, $p3, $p4, $p5, $p6, $p7)")
    ???
  }

  def arcAbs(p1: Float, p2: Float, p3: Float, p4: Boolean, p5: Boolean, p6: Float, p7: Float): Unit = {
    log(s"arcAbs($p1, $p2, $p3, $p4, $p5, $p6, $p7)")
    ???
  }

  private def pathMoveTo(x: Float, y: Float): Unit = {
    if (isFirst) {
      startX  = x
      startY  = y
      isFirst = false
    }
    out.moveTo(x, y)
  }

  private def pathLineTo(x: Float, y: Float): Unit =
    out.lineTo(x, y)

  private def pathCurveTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Unit =
    out.curveTo(x1, y1, x2, y2, x3, y3)
}
