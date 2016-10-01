package de.sciss.imperfect

import de.sciss.file._

package object notebook {
  def formatFile(in: File, args: Any*): File = {
    val outName   = in.name.format(args: _*)
    val outF      = in.parentOption.fold(file(outName))(_ / outName)
    outF
  }
}
