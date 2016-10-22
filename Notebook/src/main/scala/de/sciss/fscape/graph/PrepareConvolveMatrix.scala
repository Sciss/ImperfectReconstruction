/*
 *  PrepareConvolveMatrix.scala
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v2+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.fscape
package graph
import de.sciss.fscape.stream.{StreamIn, StreamOut}

import scala.collection.immutable.{IndexedSeq => Vec}

// cf. https://github.com/mlevans/pretty-heatmaps
// https://github.com/sergbas/ImageProcessor/blob/master/app/src/main/java/demo/apps/imageprocessor/filters/BicubicFilter.java
final case class PrepareConvolveMatrix(in: GE, rows: GE, columns: GE, kernel: GE, step: GE)
  extends UGenSource.SingleOut {

  protected def makeUGens(implicit b: UGenGraph.Builder): UGenInLike =
    unwrap(Vector(in.expand, rows.expand, columns.expand, kernel.expand, step.expand))

  protected def makeUGen(args: Vec[UGenIn])(implicit b: UGenGraph.Builder): UGenInLike =
    UGen.SingleOut(this, args)

  private[fscape] def makeStream(args: Vec[StreamIn])(implicit b: stream.Builder): StreamOut = {
    val Vec(in, rows, columns, kernel, step) = args
    ???
//    stream.PrepareConvolveMatrix(in = in.toDouble, rows = rows.toInt, columns = columns.toInt,
//      kernel = kernel.toInt, step = step.toInt)
  }
}
