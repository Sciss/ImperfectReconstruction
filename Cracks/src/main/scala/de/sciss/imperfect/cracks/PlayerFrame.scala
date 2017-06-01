package de.sciss.imperfect.cracks

import java.awt.image.BufferedImage
import java.awt.{Color, Graphics, GraphicsConfiguration, GraphicsDevice, Point, Toolkit}

final class PlayerFrame(config: Config, screen: GraphicsDevice, screenConf: GraphicsConfiguration)
  extends java.awt.Frame(null: String, screenConf) { w =>

  private[this] val imageName   = s"cracks${config.thisChannel + 1}.png"
  private[this] val urlImage    = getClass.getResource(imageName)
  private[this] val image       = if (urlImage == null) null else Toolkit.getDefaultToolkit.getImage(urlImage)

  if (image == null) println(s"Warning: could not find image resource '$imageName'")

  setUndecorated  (true)
  // setIgnoreRepaint(true)
  setBackground(new Color(config.background))

  override def paint(g: Graphics): Unit = {
    super.paint(g)
    if (image != null) {
      val width   = getWidth
      val height  = getHeight
      val x       = (width  - 1024) >> 1
      val y       = (height - 1024) >> 1
      g.drawImage(image, x, y, this)
    }
  //        val m = debugMessage
  //        if (!m.isEmpty) {
  //          g.setFont(fnt)
  //          val fm = g.getFontMetrics
  //          val tw = fm.stringWidth(debugMessage)
  //          g.setColor(Color.white)
  //          g.drawString(m, (getWidth - tw)/2, getHeight/2 - fm.getAscent)
  //        }
  }

  def fullScreen(): Unit = {
    w.setSize(screenConf.getBounds.getSize)
    screen.setFullScreenWindow(w)
    w.requestFocus()

    // "hide" cursor
    val cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val cursor = w.getToolkit.createCustomCursor(cursorImg, new Point(0, 0), "blank")
    w.setCursor(cursor)
  }
}