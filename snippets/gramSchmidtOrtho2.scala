implicit class VectorOps(x: Vector[Double]) {
  def * (scalar: Double): Vector[Double] = x.map(_ * scalar)
  def / (scalar: Double): Vector[Double] = x.map(_ / scalar)
  def + (scalar: Double): Vector[Double] = x.map(_ + scalar)
  def - (scalar: Double): Vector[Double] = x.map(_ - scalar)
  def + (y: Vector[Double]): Vector[Double] = (x, y).zipped.map(_ + _)
  def - (y: Vector[Double]): Vector[Double] = (x, y).zipped.map(_ - _)
}

def dot(u: Vector[Double], v: Vector[Double]): Double = 
  (u, v).zipped.map(_ * _).sum

def proj(u: Vector[Double], v: Vector[Double]): Vector[Double] = u * (dot(v, u) / dot(u, u))

def sum(xs: Vector[Vector[Double]]): Vector[Double] = xs.reduce(_ + _)

def length(u: Vector[Double]): Double = math.sqrt(dot(u, u))

def gramSchmidtOrtho(vs: Vector[Vector[Double]], normalize: Boolean = true): Vector[Vector[Double]] = {
  val us = (Vector.empty[Vector[Double]] /: vs) { case (uIter, vk) =>
    val uk = if (uIter.isEmpty) vk else vk - sum(uIter.map(proj(_, vk)))
    uIter :+ uk
  }

  if (!normalize) us else us.map(uk => uk / length(uk))
}

// val v1 = Vector(3.0, 1.0)
// val v2 = Vector(2.0, 2.0)
// val us = gramSchmidtOrtho(Vector(v1, v2), normalize = false)
// val es = gramSchmidtOrtho(Vector(v1, v2), normalize = true )

def mgs(vs: Vector[Vector[Double]], normalize: Boolean = true): Vector[Vector[Double]] = {
  @annotation.tailrec
  def loop(vt: Vector[Vector[Double]], res: Vector[Vector[Double]]): Vector[Vector[Double]] = 
    vt match {
      case vk +: vtt =>
        val uk = (vk /: res) { case (ukp, up) =>
          ukp - proj(up, ukp)
        }
        loop(vtt, res :+ uk)
        
      case _ => res
    }
    
  val us = loop(vs, Vector.empty)

  if (!normalize) us else us.map(uk => uk / length(uk))
}

// val usM = mgs(Vector(v1, v2), normalize = false)
// val esM = mgs(Vector(v1, v2), normalize = true )
// 
// us == usM
// es == esM

////////////////////////////////

import java.awt.image.BufferedImage

// cf. https://stackoverflow.com/questions/596216
def extractBrightness(in: BufferedImage): Vector[Vector[Double]] = {
  Vector.tabulate(in.getHeight) { y =>
    Vector.tabulate(in.getWidth) { x =>
      val rgb = in.getRGB(x, y)
      val r   = ((rgb & 0xFF0000) >> 16) / 255f
      val g   = ((rgb & 0x00FF00) >>  8) / 255f
      val b   = ( rgb & 0x0000FF       ) / 255f
      val lum = (0.299 * r.squared + 0.587 * g.squared + 0.114 * b.squared).sqrt
      lum
    }
  }
}

def fillChannel(in: Vector[Vector[Double]], out: BufferedImage, chan: Int, add: Double = 0.0, mul: Double = 1.0): Unit = {
  val shift = chan * 8
  val mask  = ~(0xFF << shift)
  var MAX = 0
  for (y <- in.indices) {
    val v = in(y)
    for (x <- v.indices) {
      val d = (v(x) + add) * mul
      val e = (d.clip(0, 1) * 0xFF + 0.5).toInt
      if (e > MAX) MAX = e
      val i = e << shift
      val j = out.getRGB(x, y)
      val k = j & mask | i
      out.setRGB(x, y, k)
    }
  }
  println(s"MAX = $MAX")
}
// val briOutN = briOut.map(row => row.map(x => x.linlin(min, max, 0, 1)))

def mkBlackImage(width: Int, height: Int): BufferedImage = {
  import java.awt.Color
  val res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
  val gTmp = res.createGraphics()
  gTmp.setColor(Color.black)
  gTmp.fillRect(0, 0, width, height)
  gTmp.dispose()
  res
}

def extractChannel(in: BufferedImage, chan: Int): Vector[Vector[Double]] = {
  val shift = chan * 8
  Vector.tabulate(in.getHeight) { y =>
    Vector.tabulate(in.getWidth) { x =>
      val i = (in.getRGB(x, y) >>> shift) & 0xFF
      i.toDouble / 0xFF
    }
  }
}

val fIn = userHome/"Documents"/"projects"/"Unlike"/"moor_8024"/"moor_8024-00059.jpg"
require(fIn.exists())
val fOut = userHome/"Documents"/"temp"/"moor_ortho.png"
require(!fOut.exists())

def run(): Unit = {
  val imgIn = javax.imageio.ImageIO.read(fIn)
  val briIn = (0 until 3).map(ch => extractChannel(imgIn, ch))
  imgIn.flush()
  
  def differentiate(in: Vector[Vector[Double]]): Vector[Vector[Double]] =
    in.map(_.differentiate)
  
  def integrate(in: Vector[Vector[Double]]): Vector[Vector[Double]] =
    in.map(_.integrate)
  
  val briOut = {
    val m = briIn.map { ch =>
//       println("--1")
      val data = mgs(ch, normalize = false)
//       println("--2")
      val min = data.map(_.min).min
      val max = data.map(_.max).max
      data.map(row => row.map(_.linlin(min, max, 0.0, 1.0)))
    }
//     val n = (briIn zip m).map { case (a +: _, b) =>
//       a +: b
//     }
//     integrate(n)
    m
  }
  
  val imgOut = mkBlackImage(imgIn.getWidth, imgIn.getHeight)
  
//       println("--3")
  briOut.zipWithIndex.foreach { case (data, ch) =>
    fillChannel(data, imgOut, chan = ch)
  }
  
  javax.imageio.ImageIO.write(imgOut, "png", fOut)
}

run()  // then: equalize + gamma 0.8
