/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package de.sciss.imperfect.difference

import java.awt._
import java.awt.image._

// quick and dirty Scala translation by IntelliJ
object RGBComposite {
  // Multiply two numbers in the range 0..255 such that 255*255=255
  def multiply255(a: Int, b: Int): Int = {
    val t = a * b + 0x80
    ((t >> 8) + t) >> 8
  }

  def clamp(a: Int): Int =
    if      (a <   0)   0
    else if (a > 255) 255
    else a

  abstract class RGBCompositeContext(var alpha: Float, var srcColorModel: ColorModel, var dstColorModel: ColorModel)
    extends CompositeContext {

    def dispose(): Unit = ()

    def composeRGB(src: Array[Int], dst: Array[Int], alpha: Float): Unit

    def compose(src: Raster, dstIn: Raster, dstOut: WritableRaster): Unit = {
      val alpha: Float = this.alpha
      var srcPix: Array[Int] = null
      var dstPix: Array[Int] = null
      val x   = dstOut.getMinX
      val w   = dstOut.getWidth
      val y0  = dstOut.getMinY
      val y1  = y0 + dstOut.getHeight

      var y = y0
      while (y < y1) {
        srcPix = src.getPixels  (x, y, w, 1, srcPix)
        dstPix = dstIn.getPixels(x, y, w, 1, dstPix)
        composeRGB(srcPix, dstPix, alpha)
        dstOut.setPixels(x, y, w, 1, dstPix)
        y += 1
      }
    }
  }
}

abstract class RGBComposite(var extraAlpha: Float = 1.0f) extends Composite {
  if (extraAlpha < 0.0f || extraAlpha > 1.0f) throw new IllegalArgumentException("RGBComposite: alpha must be between 0 and 1")

  def getAlpha: Float = extraAlpha

  override def hashCode: Int = java.lang.Float.floatToIntBits(extraAlpha)

  override def equals(o: Any): Boolean = o match {
    case c: RGBComposite => extraAlpha == c.extraAlpha
    case _ => false
  }
}
