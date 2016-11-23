package de.sciss.imperfect.fragments

import java.awt.font.{LineBreakMeasurer, TextAttribute}
import java.awt.{Color, Font, RenderingHints}
import java.awt.image.BufferedImage
import java.text.AttributedString
import javax.imageio.ImageIO

import de.sciss.file._

object Fragments {
  final val lines =
    """die Plastikfragmente müssen auf einem Fond aufgetragen werden wie Puzzleteile
      |die Zeiten, die Orte, die Lieben, die Iche
      |teilweise ist die Sicht sehr schlecht durch das Gegenlicht der Sonne, sekundenweise fahren wir praktisch blind
      |you can be intimate with a machine without getting awkward
      |your privacy is never compromised
      |machines are also not pre-gendered
      |ich höre den Klang der Ventile, über die das Gift in den Raum eingeleitet wird
      |ein schimmerndes Glasröhrchen von vielleicht zwanzig Zentimetern Länge
      |durch leichtes Eindrücken des Glasröhrchens kann man einen Schuß auslösen
      |ein kurzer Moment, in dem die Situation eskalieren kann
      |in der Form eines Eiszapfens mit leichten Verdickungen und Verdünnungen
      |ein Stamm wird gleich seinen Hinterkopf rammen
      |in der Tat muß man die Mühelosigkeit herstellen, denn sobald man sich anstrengt, erlischt dieser Platz
      |sie dreht ihren Kopf immer wieder auf bestimmte Art zur Seite
      |etwas hat den Willen, daß sie zusammenkommen, etwas soll sich zusammenbrauen
      |das Unterfangen ist aussichtslos
      |es ist eine repetitive Tätigkeit, darin liegt die Verschwörung
      |es ist unklar, in welchem Maße sein Körper und sein Geist noch unversehrt sind
      |die Bewegung darf nicht ins Stocken geraten
      |nur die fließende, stille Bewegung wird von der Macht unerkannt bleiben
      |jedes Mal, wenn etwas knirscht, halten wir bange inne und lauschen
      |wir müssen auf demselben Weg zurück, den wir hineingehen
      |wir sind eine Sonde und müssen jede Art von Schluckreflex vermeiden
      |in der Kammer selbst müssen wir diagonal laufen
      |der pure Schrecken ohne die Möglichkeit einer Beschreibung
      |all the words as a rectangular tableau or frame, always finding a wedge that becomes a corner of the frame
      |words have different magnifications (degrees thereof)
      |the texture was wax, the way wax becomes translucent when you put a light source behind it
      |a tile on the floor suddenly looks like the head of a dog
      |if one takes a ball into one's hand, it begins to disintegrate
      |the landscape is warm and cold at the same time
      |das Licht ist ganz irrsinnig
      |als ob die erschöpften Nervenstränge das Empfangene nur noch durch ein Solarisationsfilter abzubilden vermögen
      |ich lasse den Metallmenschen vom Turm springen
      |seine Metallglieder gehen beim Aufprall in die Hocke, dann richtet er sich unversehrt auf
      |a proximity field between the scynthe and the candle
      |man kann den Klang vermessen, indem man den Raum als Volumen in kleine Säulen aufteilt
      |shallow body
      |it's hard thinking about beauty when 2600 km away people are being bombed to dust
      |you were watching the perpetrator, in the next moment you become the perpetrator
      |you construct a new move, a subtle rewind
      |the true imperfection, think about it. an endlessness, until you retire
      |you escaped. you reconstructed the chase, you prolonged it
      |you reconstruct the surroundings, you divert
      |you suspend the helicopter, just for a bit, to find cover in the vegetation
      |a tiny fraction that goes beyond velocity, a tiny spatial vector
      |now an object disappears out of sight
      |you must balance the world like a waiter balances a tray
      |keep your eyes squinted, maintain a minimum blurriness
      |the vegetation had grown incredibly
      |im Innenraum ist eine weitere Abteilung, man gelangt in eine zweite Kammer
      |man ist verloren, wenn man einen Schlüssel verliert
      |I'm chasing down in my memory a person whose name I forgot
      |the image in the camera's view finder is a greyish regular structure passing by, tranquil
      |they have thus given us another person who is responsible for making the officially prescribed choreography
      |the camera is unwilling to make the official portrayal
      |der Blick pulsiert zwischen mehreren Vergrößerungsgraden
      |durch einen übermutigen Satz war ich bereits kurz auf der anderen Seite
      |the extremities, the arms and legs, are moving probabilistically around the equilibrium in order to give them a sense of liveness
      |there are gaps in the ranges of numbers, I am giving up
      |der Klang von Eiskristallen, dünnem Glas
      |im Gegenteil verliere ich die Kraft, noch irgendetwas zu sehen, der visuelle Sinn verschwindet
      |Rhythmus: sie wacht ich schlafe; sie schläft ich wache
      |its gender had changed
      |""".stripMargin.split("\n").filter(_.nonEmpty).toIndexedSeq

  assert(lines.size == 64)

  val font: Font = {
    val url   = getClass.getClassLoader.getResource("Dosis-SemiBold.ttf")
    val is    = url.openStream()
    val font  = Font.createFont(Font.TRUETYPE_FONT, is)
    is.close()
    font
  }

  def main(args: Array[String]): Unit = {
    val w         = 1024
    val h         = 1024
    val fontSize  = 92f
    val top       = 32
    val left      = 36
    val right     = 32
//    val bottom    = 32
    val colrBack  = Color.red
    val colrFront = Color.black

    val img       = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g         = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val fontD     = font.deriveFont(fontSize)
    g.setFont(fontD)

    val dirOut    = userHome / "Documents" / "projects" / "Imperfect" / "Fragments" / "slides"
    val nameOut   = "fragment-%d.png"

    lines /* .take(2) */.zipWithIndex.foreach { case (line, idx) =>

      def attempt(y0: Float): Float = {
        g.setColor(colrBack)
        g.fillRect(0, 0, w, h)
        g.setColor(colrFront)
        val as = new AttributedString(line)
        as.addAttribute(TextAttribute.FONT, fontD, 0, line.length)
        val aci = as.getIterator
        val frc = g.getFontRenderContext
        val lbm = new LineBreakMeasurer(aci, frc)

        var y = y0
        lbm.setPosition(0)
        while (lbm.getPosition < line.length) {
          val tl = lbm.nextLayout(w - left - right)
          y += tl.getAscent
          tl.draw(g, left, y)
          y += tl.getDescent + tl.getLeading
        }
        y
      }

      val y1 = attempt(0)
      val dh = h - y1
      val y2 = dh * 0.5f
      if (y2 < top) println(s"WARNING: got a top position of $y2 < $top")
      attempt(dh * 0.5f)

      val fOut = dirOut / nameOut.format(idx + 1)
      ImageIO.write(img, "png", fOut)
    }
  }
}
