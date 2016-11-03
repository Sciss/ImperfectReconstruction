/*
 *  Histo.scala
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

import java.awt.Color

import de.sciss.file._
import de.sciss.imperfect.trajectory.Stats.{DoubleStat, IntStat, ResultsDouble, ResultsInt, Stat}
import de.sciss.numbers.Implicits._
import de.sciss.serial.DataInput
import org.jfree.chart.ChartPanel
import org.jfree.data.xy.XYBarDataset

import scala.swing.{Component, Frame, GridPanel, ScrollPane, Swing}
import scalax.chart.XYChart

object Histo {
  def main(args: Array[String]): Unit = run()

  var allHisto = List.empty[Histo]

  trait Histo {
    def stat: Stat
    def bins: Array[Int]
    def center(idx: Int): Double
  }

  object HistoInt {
    def apply(name: String): HistoInt = {
      val stat    = ResultsInt(name)
      val numBins = math.min(stat.max - stat.min, 100)
      val res     = new HistoInt(stat, new Array[Int](numBins))
      allHisto  ::= res
      res
    }
  }
  class HistoInt(val stat: IntStat, val bins: Array[Int]) extends Histo {
    var min = stat.min
    var max = stat.max

    def += (i: Int): Unit = {
      val bin = (i.clip(min, max).linlin(min, max, 0, bins.length - 1) + 0.5).toInt.clip(0, bins.length)
      bins(bin) += 1
    }
    def center(i: Int): Double = (i + 0.5).linlin(0, bins.length - 1, min, max)
  }

  object HistoDouble {
    def apply(name: String): HistoDouble = {
      val stat    = ResultsDouble(name)
      val numBins = 100
      val res     = new HistoDouble(stat, new Array[Int](numBins))
      allHisto  ::= res
      res
    }
  }
  class HistoDouble(val stat: DoubleStat, val bins: Array[Int]) extends Histo {
    var min = stat.min
    var max = stat.max

    def += (i: Double): Unit = {
      val bin = (i.clip(min, max).linlin(min, max, 0, bins.length - 1) + 0.5).toInt.clip(0, bins.length)
      bins(bin) += 1
    }
    def center(i: Int): Double = (i + 0.5).linlin(0, bins.length - 1, min, max)
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
    
    val statNumPart     = HistoInt    ("numParticles")
    val statZFirst      = HistoDouble ("zFirst")
    val statZLast       = HistoDouble ("zLast")
//    val statIsBeam      = ResultsBoolean ("isBeam")
//    val statIsScat      = ResultsBoolean ("isScattered")
    val statCharge      = HistoInt    ("charge")
    val statMomentum    = HistoDouble ("momentum")
    statMomentum.max = 1
    val statNumHits     = HistoInt    ("numHits")
    val statTimeMean    = HistoDouble ("timeMean")
    statTimeMean.min = -100.0
    statTimeMean.max = +100.0
    val statTimeErr     = HistoDouble ("timeError")
    val statNumVertices = HistoInt    ("numVertices")
    val statTrajErr     = HistoDouble ("trajError")
    val statMaterial    = HistoDouble ("material")
    statMaterial.max    = 1.0
    val statPolySz      = HistoInt    ("polySz")
    val statVertexX     = HistoDouble ("vertexX")
    statVertexX.min = -10.0
    statVertexX.max = +10.0
    val statVertexY     = HistoDouble ("vertexY")
    statVertexY.min = -10.0
    statVertexY.max = +10.0
    val statVertexZ     = HistoDouble ("vertexZ")
    statVertexZ.min = -200.0
    statVertexZ.max = +200.0
    val statVertexXE    = HistoDouble ("vertexXE")
    val statVertexYE    = HistoDouble ("vertexYE")
    val statVertexZE    = HistoDouble ("vertexZE")
    val statVertexXT    = HistoDouble ("vertexXT")
    val statVertexYT    = HistoDouble ("vertexYT")
    val statVertexZT    = HistoDouble ("vertexZT")
    val statTrajX       = HistoDouble ("trajX")
    statTrajX.min = -2000.0
    statTrajX.max = +2000.0
    val statTrajY       = HistoDouble ("trajY")
    statTrajY.min = -500.0
    statTrajY.max = +500.0
    val statTrajZ       = HistoDouble ("trajZ")

    print("Analyzing... ")

    var evIdx = 0
    var lastProg = 0
    while (evIdx < events.length) {
      val ev = events(evIdx)

      val numPart = ev.particles.length
      statNumPart += numPart
      ev.particles.foreach { p =>
        statZFirst      += p.zFirst
        statZLast       += p.zLast
//        statIsBeam      += p.isBeam
//        statIsScat      += p.isScattered
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

    def mkChart(histo: Histo): XYChart = {
      val data = histo.bins.zipWithIndex.map { case (num, idx) =>
        histo.center(idx) -> num
      } .toVector
      import scalax.chart.api._

      val barWidth  = histo.center(1) - histo.center(0)
      val fihData   = data.toXYSeriesCollection(histo.stat.name)
      val withWidth = new XYBarDataset(fihData, barWidth)
      val chart     = XYBarChart(withWidth, title = s"Histogram for ${histo.stat.name}")
      val plot      = chart.plot
      plot.getRenderer.setSeriesPaint(0, Color.darkGray)
      chart
    }

    Swing.onEDT {
      val charts = allHisto.reverse.map { histo =>
        // import scalax.chart.api._
        // histo.bins.zipWithIndex.map(_.swap).toVector.toXYSeries(name = histo.stat.name)
        // plotXY(series = series, legends = legends, title = title, xlabel = "", ylabel = ylabel)
        mkChart(histo)
      }

      val numRows = charts.size
      val numCols = 1
      val panel = new GridPanel(numRows, numCols) {
        vGap  = 24
        hGap  = 24
        contents ++= charts.map(c => Component.wrap(new ChartPanel(c.peer, false))) // useBuffer = false for PDF export
      }

      /* val frame = */ new Frame {
        contents = new ScrollPane(panel)
        // PDFSupport.addMenu(peer, panel.peer :: Nil)
        pack().centerOnScreen()
        open()
      }
    }
  }
}