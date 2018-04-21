package de.sciss.imperfect.difference

import de.sciss.file._

object NameComparisonTest {
  def main(args: Array[String]): Unit = {
    val baseDirInt: File    = file("/") / "data" / "projects" / "Imperfect"
    val coverDir  : File    = baseDirInt / "catalog" / "cover"
    val hhDir     : File    = coverDir / "site-2out_catalog"
    val hhExt     : String  = "jpg"

    val hhIn0     = hhDir   .children(_.ext == hhExt).toList

    import MakeCatalogCover.fileNameOrdering

    val ord       = fileNameOrdering // implicitly[Ordering[File]]

    def testTwo(a: File, b: File): Unit = {
      val c1 = ord.compare(a, b)
      val c2 = ord.compare(b, a)

      def fail: String = s"${a.name} -- ${b.name}, $c1, $c2)"

      require(c1 != 0  , fail)
      require(c2 != 0  , fail)
      require(c1 == -c2, fail)
    }

    def testThree(a: File, b: File, c: File): Unit = {
      val c1 = ord.compare(a, b)
      val c2 = ord.compare(a, c)
      val c3 = ord.compare(b, c)

      def fail: String = s"${a.name} -- ${b.name} -- ${c.name}, $c1, $c2, $c3)"

      require(c1 != 0  , fail)
      require(c2 != 0  , fail)
      require(c3 != 0  , fail)

      if (c1 < 0) {
        if (c2 < 0) {
          //
        } else {
          require(c3 > 0, fail)
        }
      } else {
        if (c2 < 0) {
          require (c3 < 0, fail)
        }
      }
    }

    def loop(a: File, tail: List[File]): Unit = {
      tail.foreach { b =>
        testTwo(a, b)
      }

      tail match {
        case h1 :: t1 =>
          loop(h1, t1)

        case _ =>
      }
    }

    require(hhIn0.nonEmpty)
//    loop(hhIn0.head, hhIn0.tail)
//    println("Succeeded.")

//    println(s"size = ${hhIn0.size}")
//
//    val rnd = new util.Random(0L)
//    for (_ <- 1 to 10000) {
//      val test = rnd.shuffle(hhIn0)
//      try {
//        test.sorted
//      } catch {
//        case NonFatal(_) =>
//          println("FAILED:")
//          test.foreach(f => println(f.name))
//          sys.exit(1)
//      }
//    }

//    hhIn0.take(20).permutations.foreach { list =>
//      list.sorted
//    }

    val names = Vector(
      "frame-18.jpg",
      "frame-128.jpg",
      "frame-111.jpg",
      "frame-187.jpg",
      "frame-120.jpg",
      "frame-17.jpg",
      "frame-126.jpg",
      "frame-268.jpg",
      "frame-106.jpg",
      "frame-78.jpg",
      "frame-101.jpg",
      "frame-69.jpg",
      "frame-170.jpg",
      "frame-197.jpg",
      "frame-80.jpg",
      "frame-173.jpg",
      "frame-92.jpg",
      "frame-81.jpg",
      "frame-91.jpg",
      "frame-232.jpg",
      "frame-77.jpg",
      "frame-118.jpg",
      "frame-1.jpg",
      "frame-241.jpg",
      "frame-129.jpg",
      "frame-64.jpg",
      "frame-93.jpg",
      "frame-258.jpg",
      "frame-110.jpg",
      "frame-145.jpg",
      "frame-253.jpg",
      "frame-16.jpg",
      "frame-25.jpg",
      "frame-200.jpg",
      "frame-138.jpg",
      "frame-206.jpg",
      "frame-4.jpg",
      "frame-162.jpg"
    )
    val files = names.map(file(_)).toList
    println(files.size)
//    loop(files.head, files.tail)
    files.combinations(3).foreach { case a :: b :: c :: Nil =>
      testThree(a, b, c)
    }

//    loop(files.reverse.head, files.reverse.tail)
    files.sorted(ord)

    val res = foo.Foo.compareNames("11", "100")

    require(res < 0)
  }
}