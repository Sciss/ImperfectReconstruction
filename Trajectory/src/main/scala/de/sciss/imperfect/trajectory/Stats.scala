/*
 *  Stats.scala
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

import de.sciss.file._
import de.sciss.serial.DataInput

import scala.collection.breakOut

object Stats {
  def main(args: Array[String]): Unit = run()

  var allStats = List.empty[Stat]

  lazy val Results = Seq(
    IntStat("numParticles", min = 3, max = 20, sum = 498551, count = 80260, mean = 6.21170),
    DoubleStat("zFirst", min = -760.000, max = 1500.49, sum = 2669800.920791626, count = 498551, mean = 5.35512),
    DoubleStat("zLast", min = -351.650, max = 5100.50, sum = 8.597991611595917E8, count = 498551, mean = 1724.60),
    BooleanStat("isBeam", sum = 89948, count = 498551, mean = 0.180419),
    BooleanStat("isScattered", sum = 71941, count = 498551, mean = 0.144300),
    IntStat("charge", min = -1, max = 1, sum = 250023, count = 498551, mean = 0.501499),
    DoubleStat("momentum", min = 0.398045, max = 42812.7, sum = 3.505121785991126E7, count = 498551, mean = 70.3062),
    IntStat("numHits", min = 9, max = 126, sum = 22243120, count = 498551, mean = 44.6155),
    DoubleStat("timeMean", min = -50.3818, max = 9999.00, sum = 769718.6691389416, count = 498551, mean = 1.54391),
    DoubleStat("timeError", min = 0.242474, max = 20.0000, sum = 857610.401319623, count = 498472, mean = 1.72048),
    IntStat("numVertices", min = 0, max = 13, sum = 755898, count = 498551, mean = 1.51619),
    DoubleStat("trajError", min = 0.0180405, max = 55.8787, sum = 1138542.4392659701, count = 498551, mean = 2.28370),
    DoubleStat("material", min = 0.0212994, max = 203.451, sum = 8915523.991351403, count = 408603, mean = 21.8195),
    IntStat("polySz", min = 6, max = 120, sum = 43430759, count = 498551, mean = 87.1140),
    DoubleStat("vertexX", min = -79.4470, max = 81.5722, sum = 126688.40466001442, count = 755898, mean = 0.167600),
    DoubleStat("vertexY", min = -35.2266, max = 71.8446, sum = -20739.80552002787, count = 755898, mean = -0.0274373),
    DoubleStat("vertexZ", min = -972.524, max = 932.334, sum = -1.5457406828147843E7, count = 755898, mean = -20.4491),
    DoubleStat("vertexXE", min = 0.00208519, max = 0.199783, sum = 34665.36691578035, count = 755898, mean = 0.0458598),
    DoubleStat("vertexYE", min = 0.00139083, max = 0.199783, sum = 34983.21579597413, count = 755898, mean = 0.0462803),
    DoubleStat("vertexZE", min = 0.0361758, max = 49.7038, sum = 1497046.7732263915, count = 755898, mean = 1.98049),
    DoubleStat("vertexXT", min = 0.00208519, max = 0.199783, sum = 34665.36691578035, count = 755898, mean = 0.0458598),
    DoubleStat("vertexYT", min = 0.00139083, max = 0.199783, sum = 34983.21579597413, count = 755898, mean = 0.0462803),
    DoubleStat("vertexZT", min = 5.12483e-06, max = 0.115367, sum = 636.4954458093011, count = 755898, mean = 0.000842039),
    DoubleStat("trajX", min = -9252.42, max = 8050.15, sum = 5.3662477281571925E8, count = 43430759, mean = 12.3559),
    DoubleStat("trajY", min = -1916.97, max = 1709.94, sum = 1.771272915623202E7, count = 43430759, mean = 0.407838),
    DoubleStat("trajZ", min = -972.524, max = 5000.00, sum = 1.028547489362591E11, count = 43430759, mean = 2368.25)
  )

  lazy val ResultsInt    : Map[String, IntStat]      = Results.collect { case s: IntStat     => s.name -> s } (breakOut)
  lazy val ResultsDouble : Map[String, DoubleStat]   = Results.collect { case s: DoubleStat  => s.name -> s } (breakOut)
  lazy val ResultsBoolean: Map[String, BooleanStat]  = Results.collect { case s: BooleanStat => s.name -> s } (breakOut)

  trait Stat {
    def name: String
  }

  object IntStat {
    def apply(name: String, min: Int = Int.MaxValue, max: Int = Int.MinValue,
              sum: Long = 0L, count: Int = 0, mean: Double = Double.NaN): IntStat = {
      val res = new IntStat(name, min = min, max = max, sum = sum, count = count)
      allStats ::= res
      res
    }
  }
  final class IntStat private (val name: String, var min: Int = Int.MaxValue, var max: Int = Int.MinValue,
                      var sum: Long = 0L, var count: Int = 0) extends Stat {
    def mean: Double = sum.toDouble / count

    def +=(i: Int): Unit = {
      if (i < min) min = i
      if (i > max) max = i
      sum   += i
      count += 1
    }

    override def toString = f"""IntStat("$name", min = $min, max = $max, sum = $sum, count = $count, mean = $mean%g)"""
  }

  object DoubleStat {
    def apply(name: String, min: Double = Double.MaxValue, max: Double = Double.MinValue,
              sum: Double = 0.0, count: Int = 0, mean: Double = Double.NaN): DoubleStat = {
      val res = new DoubleStat(name, min = min, max = max, sum = sum, count = count)
      allStats ::= res
      res
    }
  }
  final class DoubleStat private (val name: String, var min: Double = Double.MaxValue, var max: Double = Double.MinValue,
                         var sum: Double = 0.0, var count: Int = 0) extends Stat {
    def mean: Double = sum / count
    
    def +=(i: Double): Unit = {
      if (i < min) min = i
      if (i > max) max = i
      sum   += i
      count += 1
    }

    override def toString = f"""DoubleStat("$name", min = $min%g, max = $max%g, sum = $sum, count = $count, mean = $mean%g)"""
  }

  object BooleanStat {
    def apply(name: String, sum: Int = 0, count: Int = 0, mean: Double = Double.NaN): BooleanStat = {
      val res = new BooleanStat(name, sum = sum, count = count)
      allStats ::= res
      res
    }
  }
  final class BooleanStat private (val name: String, var sum: Int = 0, var count: Int = 0) extends Stat {
    def mean: Double = sum.toDouble / count

    def +=(i: Boolean): Unit = {
      if (i) sum += 1
      count += 1
    }

    override def toString = f"""BooleanStat("$name", sum = $sum, count = $count, mean = $mean%g)"""
  }

  def run(): Unit = {
    val events = Events.readStd()

    val statNumPart     = IntStat     ("numParticles")
    val statZFirst      = DoubleStat  ("zFirst")
    val statZLast       = DoubleStat  ("zLast")
    val statIsBeam      = BooleanStat ("isBeam")
    val statIsScat      = BooleanStat ("isScattered")
    val statCharge      = IntStat     ("charge")
    val statMomentum    = DoubleStat  ("momentum")
    val statNumHits     = IntStat     ("numHits")
    val statTimeMean    = DoubleStat  ("timeMean")
    val statTimeErr     = DoubleStat  ("timeError")
    val statNumVertices = IntStat     ("numVertices")
    val statTrajErr     = DoubleStat  ("trajError")
    val statMaterial    = DoubleStat  ("material")
    val statPolySz      = IntStat     ("polySz")
    val statVertexX     = DoubleStat  ("vertexX")
    val statVertexY     = DoubleStat  ("vertexY")
    val statVertexZ     = DoubleStat  ("vertexZ")
    val statVertexXE    = DoubleStat  ("vertexXE")
    val statVertexYE    = DoubleStat  ("vertexYE")
    val statVertexZE    = DoubleStat  ("vertexZE")
    val statVertexXT    = DoubleStat  ("vertexXT")
    val statVertexYT    = DoubleStat  ("vertexYT")
    val statVertexZT    = DoubleStat  ("vertexZT")
    val statTrajX       = DoubleStat  ("trajX")
    val statTrajY       = DoubleStat  ("trajY")
    val statTrajZ       = DoubleStat  ("trajZ")

    print("Analyzing... ")

    var evIdx = 0
    var lastProg = 0
    while (evIdx < events.length) {
      val ev = events(evIdx)

      val numPart = ev.particles.length
      statNumPart += numPart
      ev.particles.foreach { p =>
//        zFirst: Float, zLast: Float, isBeam: Boolean, isScattered: Boolean, charge: Int,
//        momentum: Float, numHits: Int, timeMean: Float, timeError: Float,
//        vertices: Array[Vertex], trajError: Float, material: Float, traj: Array[Point3D]
        
        statZFirst      += p.zFirst
        statZLast       += p.zLast
        statIsBeam      += p.isBeam
        statIsScat      += p.isScattered
        statCharge      += p.charge
        statMomentum    += p.momentum
        statNumHits     += p.numHits
        statTimeMean    += p.timeMean
        if (p.timeError >= 0) statTimeErr += p.timeError
        statNumVertices += p.vertices.length
        statTrajErr     += p.trajError
        if (p.material >= 0) statMaterial += p.material
        statPolySz      += p.traj.length
        
        p.vertices.foreach { v =>
          statVertexX += v.point.x
          statVertexY += v.point.y
          statVertexZ += v.point.z

          statVertexXE += v.error.x
          statVertexYE += v.error.y
          statVertexZE += v.error.z

          statVertexXT += v.trajError.x
          statVertexYT += v.trajError.y
          statVertexZT += v.trajError.z
        }
        
        p.traj.foreach { point =>
          statTrajX += point.x
          statTrajY += point.y
          statTrajZ += point.z
        }
      }

      evIdx += 1
      val newProg = evIdx * 100 / events.length
      while (lastProg < newProg) {
        print('#')
        lastProg += 1
      }
    }

    println(" Done.")

    allStats.reverse.foreach(println)
  }
}