package de.sciss.imperfect.trajectory

import de.sciss.kollflitz.Vec

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
object Event {
  private[this] val RegEvent        = """Event (\d+) in Spill (\d+)""".r
  private[this] val RegNumPart      = """Number of Particles (\d+)""".r
  private[this] val RegPartNum      = """Particle number (\d+)""".r
  private[this] val RegZFirstLast   = """ZFirst (-?\d+[.]?\d+) ZLast (-?\d+[.]\d+)""".r
  private[this] val RegIsBeam       = """Is beam ([01])""".r
  private[this] val RegIsScattered  = """Is scattered beam ([01])""".r
  private[this] val RegCharge       = """Charge (-?[1])""".r
  private[this] val RegMass         = """Reconstructed mass ([-+]?\d+|[-+]?\d+[.]\d+)""".r
  private[this] val RegMomentum     = """Momentum (-?\d+[.]?\d+)""".r
  private[this] val RegNumHits      = """number of hits (\d+)""".r
  private[this] val RegTimeMeanErr  = """mean time (-?\d+[.]?\d+) time error (\d+[.]?\d+)""".r
  private[this] val RegNumVertices  = """associated with (\d+) vertices:""".r
  private[this] val RegVertexPos    = """(-?\d+[.]?\d+),(-?\d+[.]?\d+),(-?\d+[.]?\d+) Enter""".r
  private[this] val RegVertexErr    = """Error x, y, z for Vertex (\d+[.]?\d+) (\d+[.]?\d+) (\d+[.]?\d+)""".r
  private[this] val RegVertexTraj   = """Error x, y, z for Traj at Vertex (\d+[.]?\d+) (\d+[.]?\d+) (\d+[.]?\d+)""".r
  private[this] val RegTrajRecon    = """Trajectory reconstruction error (\d+[.]?\d+)""".r
  private[this] val RegMaterial     = """Material traversed (\d+[.]?\d+)""".r
  private[this] val RegPoly         = """polyline ((-?\d+[.]?\d+),(-?\d+[.]?\d+),(-?\d+[.]?\d+)\s)+Enter""".r
  private[this] val RegPolyPt       = """(-?\d+[.]?\d+),(-?\d+[.]?\d+),(-?\d+[.]?\d+)\s""".r

  def parse(source: io.Source): Vec[Event] = {
    val b   = Vector.newBuilder[Event]
    val ln  = source.getLines()
    while (ln.hasNext) {
      val RegEvent(idS, spillS)   = ln.next()
      val id    = idS   .toInt
      val spill = spillS.toInt
      val RegNumPart(numParticlesS) = ln.next()
      val numParticles = numParticlesS.toInt
      val particles = for (i <- 0 until numParticles) yield {
        val lnDash = ln.next().trim
        require(lnDash == "===========", s"'$lnDash'")
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
        val RegPoly(polyS @ _*) = ln.next()
        val traj: Vec[Point3D] = polyS.map { s =>
          val RegPolyPt(pxS, pyS, pzS) = s
          val px = pxS.toFloat; val py = pyS.toFloat; val pz = pzS.toFloat
          Point3D(px, py, pz)
        } (breakOut)
        Particle(zFirst = zFirst, zLast = zLast, isBeam = isBeam, isScattered = isScattered, charge = charge,
          momentum = momentum, numHits = numHits, timeMean = timeMean, timeError = timeError,
          vertices = vertices, trajError = trajError, material = material, traj = traj)
      }
      require(ln.next() == "")
      val evt = Event(id = id, spill = spill, particles = particles)
      b += evt
    }
    b.result()
  }
}
final case class Event(id: Int, spill: Int, particles: Vec[Particle])

final case class Particle(zFirst: Float, zLast: Float, isBeam: Boolean, isScattered: Boolean, charge: Int,
                          /* mass: Int, */ momentum: Float, numHits: Int, timeMean: Float, timeError: Float,
                          vertices: Vec[Vertex], trajError: Float, material: Float, traj: Vec[Point3D])

final case class Vertex(point: Point3D, error: Point3D, trajError: Point3D)

final case class Point3D(x: Float, y: Float, z: Float)