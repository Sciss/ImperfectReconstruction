/*
 *  Event.scala
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

import de.sciss.kollflitz.Vec
import de.sciss.serial.{DataInput, DataOutput}

import scala.collection.breakOut

/*
Event <INT 31287> in Spill <INT 197>
Number of Particles <INT N 4>
===========
Particle number <INT I IN 0 UNTIL N>
ZFirst <FLOAT -760> ZLast <FLOAT -288.3>
Is beam <ZERO-OR-ONE 1>
Is scattered beam <ZERO-OR-ONE 0>
Charge <MINUS-ONE-OR-ONE 1>
Reconstructed mass <MINUS-ONE -1>
Momentum <FLOAT 152.362>
number of hits <INT 16>
mean time <FLOAT 0.694756> time error <FLOAT 0.5>
associated with <INT M 2> vertices:
<FLOAT 0.429564>,<FLOAT -0.533376>,<FLOAT 14.3875> Enter
Error x, y, z for Vertex <FLOAT 0.00828028> <FLOAT 0.0129703> <FLOAT 0.943455>
Error x, y, z for Traj at Vertex <FLOAT 0.00828028> <FLOAT 0.0129703> <FLOAT 2.09863e-05>
... (REPEAT M TIMES)
Trajectory reconstruction error <FLOAT 0.440973>
Material traversed <MINUS-ONE OR FLOAT -1>
trajectory:
polyline <FLOAT 0.448035>,<FLOAT -0.493576>,<FLOAT 17.2327> ... (etc.) 0.364008,-0.492101,-32.7673 0.259136,-0.490627,-82.7673 0.134965,-0.489158,-132.767 -0.000202844,-0.487636,-182.767 -0.134938,-0.486105,-232.767 -0.269673,-0.484573,-282.767 -0.404409,-0.483042,-332.767 -0.539144,-0.48151,-382.767 -0.673879,-0.479979,-432.767 -0.808614,-0.478448,-482.767 Enter
Enter
===========
Particle number <INT I IN 0 UNTIL N>
...

 */
object Events {
  private[this] val rInt            = """\d+"""
  private[this] val rSignum         = """[-]?[1]"""
  private[this] val rBinary         = """[01]"""
  private[this] val rPosFloat       = """\d+(?:[.]\d+)?(?:[e][-]?\d+)?"""
  private[this] val rFloat          = s"""[-+]?$rPosFloat"""
  private[this] val rPosFloatOpt    = s"""[-][1]|$rPosFloat"""

  private[this] val RegEvent        = s"""Event ($rInt) in Spill ($rInt)""".r
  private[this] val RegNumPart      = s"""Number of Particles ($rInt)""".r
  private[this] val RegPartNum      = s"""Particle number ($rInt)""".r
  private[this] val RegZFirstLast   = s"""ZFirst ($rFloat) ZLast ($rFloat)""".r
  private[this] val RegIsBeam       = s"""Is beam ($rBinary)""".r
  private[this] val RegIsScattered  = s"""Is scattered beam ($rBinary)""".r
  private[this] val RegCharge       = s"""Charge ($rSignum)""".r
  private[this] val RegMass         = s"""Reconstructed mass ($rFloat)""".r
  private[this] val RegMomentum     = s"""Momentum ($rFloat)""".r
  private[this] val RegNumHits      = s"""number of hits ($rInt)""".r
  private[this] val RegTimeMeanErr  = s"""mean time ($rFloat) time error ($rPosFloatOpt)""".r
  private[this] val RegNumVertices  = s"""associated with ($rInt) vertices:""".r
  private[this] val RegVertexPos    = s"""($rFloat),($rFloat),($rFloat) Enter""".r
  private[this] val RegVertexErr    = s"""Error x, y, z for Vertex ($rFloat) ($rFloat) ($rFloat)""".r
  private[this] val RegVertexTraj   = s"""Error x, y, z for Traj at Vertex ($rPosFloat) ($rPosFloat) ($rPosFloat)""".r
  private[this] val RegTrajRecon    = s"""Trajectory reconstruction error ($rPosFloat)""".r
  private[this] val RegMaterial     = s"""Material traversed ($rPosFloatOpt)""".r
//  private[this] val RegPoly         = """polyline ((-?\d+[.]?\d+),(-?\d+[.]?\d+),(-?\d+[.]?\d+)\s)+Enter""".r
  private[this] val RegPoly         = """polyline (.+) Enter""".r
  private[this] val RegPolyPt       = s"""($rFloat),($rFloat),($rFloat)""".r

  def parse(source: io.Source): Vec[Event] = {
    val b   = Vector.newBuilder[Event]
    val ln  = source.getLines()
    while (ln.hasNext) {
      val RegEvent(idS, spillS)   = ln.next()
      val id    = idS   .toInt
      val spill = spillS.toInt
      val RegNumPart(numParticlesS) = ln.next()
      val numParticles = numParticlesS.toInt
      val lnDash = ln.next().trim
      require(lnDash == "===========", s"'$lnDash'")
      val particles = for (i <- 0 until numParticles) yield {
        val RegPartNum(partIdS) = ln.next()
        val partId = partIdS.toInt
        require(partId == i)
        val RegZFirstLast(zFirstS, zLastS) = ln.next()
        val zFirst = zFirstS.toFloat
        val zLast  = zLastS .toFloat
        val RegIsBeam(isBeamS) = ln.next()
        val isBeam = isBeamS.toInt != 0
        val RegIsScattered(isScatteredS) = ln.next()
        val isScattered = isScatteredS.toInt != 0
        val RegCharge(chargeS) = ln.next()
        val charge = chargeS.toInt
        val RegMass(massS) = ln.next()
        val mass = massS.toInt
        require(mass == -1)
        val RegMomentum(momentumS) = ln.next()
        val momentum = momentumS.toFloat
        val RegNumHits(numHitsS) = ln.next()
        val numHits = numHitsS.toInt
        val RegTimeMeanErr(timeMeanS, timeErrorS) = ln.next()
        val timeMean  = timeMeanS .toFloat
        val timeError = timeErrorS.toFloat
        val RegNumVertices(numVerticesS) = ln.next()
        val numVertices = numVerticesS.toInt
        val vertices = for (j <- 0 until numVertices) yield {
          val RegVertexPos(vxS , vyS , vzS ) = ln.next()
          val vx = vxS.toFloat; val vy = vyS.toFloat; val vz = vzS.toFloat
          val v = Point3D(vx, vy, vz)
          val RegVertexErr(vxeS, vyeS, vzeS) = ln.next()
          val vxe = vxeS.toFloat; val vye = vyeS.toFloat; val vze = vzeS.toFloat
          val ve = Point3D(vxe, vye, vze)
          val RegVertexTraj(vxtS, vytS, vztS) = ln.next()
          val vxt = vxtS.toFloat; val vyt = vytS.toFloat; val vzt = vztS.toFloat
          val vt = Point3D(vxt, vyt, vzt)
          Vertex(v, ve, vt)
        }
        val RegTrajRecon(trajErrorS) = ln.next()
        val trajError = trajErrorS.toFloat
        val RegMaterial(materialS) = ln.next()
        val material = materialS.toFloat
        require(ln.next() == """trajectory:""")
        val RegPoly(polyS) = ln.next()
        val traj: Vec[Point3D] = polyS.split("""\s""").map { s =>
          val RegPolyPt(pxS, pyS, pzS) = s
          val px = pxS.toFloat; val py = pyS.toFloat; val pz = pzS.toFloat
          Point3D(px, py, pz)
        } (breakOut)
        val lnDash = ln.next().trim
        require(lnDash == "===========", s"'$lnDash'")
        Particle(zFirst = zFirst, zLast = zLast, isBeam = isBeam, isScattered = isScattered, charge = charge,
          momentum = momentum, numHits = numHits, timeMean = timeMean, timeError = timeError,
          vertices = vertices, trajError = trajError, material = material, traj = traj)
      }
      val lnBlank = ln.next()
      require(lnBlank == "", s"'$lnBlank'")
      val evt = Event(id = id, spill = spill, particles = particles)
      b += evt
    }
    b.result()
  }

  def read(in: DataInput): Vec[Event] = {
    val numEvents = in.readInt()
//    Vector.fill(numEvents)(Event.read(in))
    Vector.tabulate(numEvents) { i =>
      if (i % 800 == 0) print('.')
      Event.read(in)
    }
  }

  def write(xs: Vec[Event], out: DataOutput): Unit = {
    out.writeInt(xs.size)
    xs.foreach(Event.write(_, out))
  }
}

object Event {
  private[this] val COOKIE = 0x4576

  def read(in: DataInput): Event = {
    val cookie        = in.readShort()
    if (cookie != COOKIE) sys.error(s"Expected cookie ${COOKIE.toHexString} but found ${cookie.toHexString}")
    val id            = in.readInt()
    val spill         = in.readShort()
    val numParticles  = in.readShort()
    val particles     = Vector.fill(numParticles)(Particle.read(in))
    Event(id = id, spill = spill, particles = particles)
  }
  
  def write(e: Event, out: DataOutput): Unit = {
    import e._
    out.writeShort(COOKIE)
    out.writeInt(id)
    out.writeShort(spill)
    out.writeShort(particles.size)
    particles.foreach(Particle.write(_, out))
  }
}
final case class Event(id: Int, spill: Int, particles: Vec[Particle])

object Particle {
  def read(in: DataInput): Particle = {
    val zFirst      = in.readFloat()
    val zLast       = in.readFloat()
    val isBeam      = in.readBoolean()
    val isScattered = in.readBoolean()
    val charge      = in.readByte()
    val momentum    = in.readFloat()
    val numHits     = in.readShort()
    val timeMean    = in.readFloat()
    val timeError   = in.readFloat()
    val numVertices = in.readShort()
    val vertices    = Vector.fill(numVertices)(Vertex.read(in))
    val trajError   = in.readFloat()
    val material    = in.readFloat()
    val polySz      = in.readShort()
    val traj        = Vector.fill(polySz)(Point3D.read(in))
    Particle(zFirst = zFirst, zLast = zLast, isBeam = isBeam, isScattered = isScattered, charge = charge,
      momentum = momentum, numHits = numHits, timeMean = timeMean, timeError = timeError,
      vertices = vertices, trajError = trajError, material = material, traj = traj)
  }
  
  def write(p: Particle, out: DataOutput): Unit = {
    import p._
    out.writeFloat  (zFirst      )
    out.writeFloat  (zLast       )
    out.writeBoolean(isBeam      )
    out.writeBoolean(isScattered )
    out.writeByte   (charge      )
    out.writeFloat  (momentum    )
    out.writeShort  (numHits     )
    out.writeFloat  (timeMean    )
    out.writeFloat  (timeError   )
    out.writeShort  (vertices.size)
    vertices.foreach(Vertex.write(_, out))
    out.writeFloat  (trajError   )
    out.writeFloat  (material    )
    out.writeShort  (traj.size)
    traj.foreach(Point3D.write(_, out))
  }
}
final case class Particle(zFirst: Float, zLast: Float, isBeam: Boolean, isScattered: Boolean, charge: Int,
                          /* mass: Int, */ momentum: Float, numHits: Int, timeMean: Float, timeError: Float,
                          vertices: Vec[Vertex], trajError: Float, material: Float, traj: Vec[Point3D])

object Vertex {
  def read(in: DataInput): Vertex = {
    val point     = Point3D.read(in)
    val error     = Point3D.read(in)
    val trajError = Point3D.read(in)
    Vertex(point = point, error = error, trajError = trajError)
  }

  def write(v: Vertex, out: DataOutput): Unit = {
    Point3D.write(v.point    , out)
    Point3D.write(v.error    , out)
    Point3D.write(v.trajError, out)
  }
}
final case class Vertex(point: Point3D, error: Point3D, trajError: Point3D)

object Point3D {
  def read(in: DataInput): Point3D = {
    val x = in.readFloat()
    val y = in.readFloat()
    val z = in.readFloat()
    Point3D(x, y, z)
  }
  
  def write(p: Point3D, out: DataOutput): Unit = {
    out.writeFloat(p.x)
    out.writeFloat(p.y)
    out.writeFloat(p.z)
  }
}
final case class Point3D(x: Float, y: Float, z: Float)