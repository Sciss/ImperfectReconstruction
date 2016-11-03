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

object Stats {
  def main(args: Array[String]): Unit = run()

  var allStats = List.empty[Stat]

  trait Stat {
    def name: String
  }

  object IntStat {
    def apply(name: String): IntStat = {
      val res = new IntStat(name)
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
    def apply(name: String): DoubleStat = {
      val res = new DoubleStat(name)
      allStats ::= res
      res
    }
  }
  final class DoubleStat private (val name: String, var min: Double = Double.MaxValue, var max: Double = Double.MinValue,
                         var sum: Double = 0L, var count: Int = 0) extends Stat {
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
    def apply(name: String): BooleanStat = {
      val res = new BooleanStat(name)
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
    val fIn = userHome / "Documents" / "projects" / "Imperfect" / "cern_daten" / "CERN_trajectories.bin"
    val dIn = DataInput.open(fIn)

    print("Reading... ")

    val events = try {
      Events.read(dIn)
    } finally {
      dIn.close()
    }
    println("Done.")

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