package de.sciss.imperfect.raspiplayer

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.annotation.tailrec
import scala.util.Random

object StrangeUrn {
  def apply[A](set: Set[A])(implicit random: Random): StrangeUrn[A] = {
    require(set.nonEmpty)
    new Impl(in = random.shuffle(set.toIndexedSeq), out = Vector.empty)
  }

  private final class Impl[A](in: Vec[A], out: Vec[A]) extends StrangeUrn[A] {
    def choose()(implicit random: Random): (A, StrangeUrn[A]) = in match {
      case head +: tail => (head, new Impl(tail, out :+ head))
      case _ =>
        @tailrec
        def loop(outRem: Vec[A], inAcc: Vec[A]): Vec[A] = outRem match {
          case a +: b +: tail =>
            val (chosen, outRem1) = if (Util.coin()) (a, b +: tail) else (b, a +: tail)
            loop(outRem1, inAcc :+ chosen)

          case head +: tail => loop(tail, inAcc :+ head)
          case _ => inAcc
        }

        val newIn  = loop(out, Vector.empty)
        val newUrn = new Impl(newIn, Vector.empty)
        newUrn.choose()
    }
  }
}
trait StrangeUrn[+A] {
  def choose()(implicit random: Random): (A, StrangeUrn[A])
}
