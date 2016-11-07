import scala.util.Try

val baseDir = file("/media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/precious")
val fwdDirs = baseDir.children(f => f.isDirectory && Try(f.name.toInt).isSuccess)
fwdDirs.foreach { fwdDir =>
  val id = fwdDir.name.toInt
  val bwdDir = baseDir / s"${id}b"
  if (!bwdDir.exists() || bwdDir.children.isEmpty) {
    val fwdFs = fwdDir.children(f => f.name.startsWith("frame-") && f.ext == "png")
    // ln -sr ../{id}/{fin}
    
    def fileId(f: File): Int = {
      val n = f.name
      val s = n.substring(n.indexOf('-') + 1, n.indexOf('.'))
      s.toInt
    }
    
    def mkName(id: Int): String = s"frame-$id.png"
    
    val fwdIds = fwdFs.map(fileId)
    val idStart = fwdIds.min
    val idEnd   = fwdIds.max
    require(idStart == 1 && idEnd == fwdIds.length)
    
    bwdDir.mkdir()
    for (idOut <- idStart to idEnd) {
      val idIn = idEnd - idOut + 1
      import sys.process._
      val cmd = Seq("ln", "-sr", s"${fwdDir.name}/${mkName(idIn)}",
                                 s"${bwdDir.name}/${mkName(idOut)}")
      // println(cmd)
      Process(cmd, baseDir).!!
    }
  }
}
