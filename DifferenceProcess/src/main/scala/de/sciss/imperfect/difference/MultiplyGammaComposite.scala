package de.sciss.imperfect.difference

import java.awt.{CompositeContext, RenderingHints}
import java.awt.image.ColorModel

import de.sciss.imperfect.difference.RGBComposite.RGBCompositeContext

object MultiplyGammaComposite {
  final class Context(alpha: Float, gamma: Float, srcColorModel: ColorModel, dstColorModel: ColorModel) 
    extends RGBCompositeContext(alpha, srcColorModel, dstColorModel) {
    
    def composeRGB(src: Array[Int], dst: Array[Int], alpha: Float): Unit = {
      val w = math.min(src.length, dst.length)
      var i = 0
      val p = 1.0f / gamma
      while (i < w) {
        val sr  = src(i)     / 255f
        val dir = dst(i)     / 255f
        val sg  = src(i + 1) / 255f
        val dig = dst(i + 1) / 255f
        val sb  = src(i + 2) / 255f
        val dib = dst(i + 2) / 255f
//        val sa  = src(i + 3) / 255f
//        val dia = dst(i + 3) / 255f

        val dor = (math.pow(dir * sr, p) * 255f + 0.5).toInt
        val dog = (math.pow(dig * sg, p) * 255f + 0.5).toInt
        val dob = (math.pow(dib * sb, p) * 255f + 0.5).toInt

//        val a : Float = alpha * sa / 255f
//        val ac: Float = 1 - a

//        dst(i)      = (a  * dor   + ac  * dir).toInt
//        dst(i + 1)  = (a  * dog   + ac  * dig).toInt
//        dst(i + 2)  = (a  * dob   + ac  * dib).toInt
//        dst(i + 3)  = (sa * alpha + dia * ac ).toInt

        dst(i)      = dor
        dst(i + 1)  = dog
        dst(i + 2)  = dob

        i += 3
      }
    }
  }
}

final class MultiplyGammaComposite(val alpha: Float = 1f, val gamma: Float = 1f) extends RGBComposite(alpha) {
  def createContext(srcColorModel: ColorModel, dstColorModel: ColorModel, hints: RenderingHints): CompositeContext =
    new MultiplyGammaComposite.Context(alpha, gamma, srcColorModel, dstColorModel)
}