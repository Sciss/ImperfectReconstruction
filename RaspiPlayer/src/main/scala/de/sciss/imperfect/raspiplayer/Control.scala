/*
 *  Control.scala
 *  (Imperfect Reconstruction)
 *
 *  Copyright (c) 2016-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v2+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.imperfect.raspiplayer

import java.net.{InetSocketAddress, SocketAddress}

import de.sciss.osc
import de.sciss.osc.{Packet, UDP}

import scala.util.Random
import scala.util.control.NonFatal

final class Control(config: Config)(implicit screens: Screens) {
  import config._

  private[this] implicit val random: Random = new Random()

  private[this] val clients: Array[InetSocketAddress] = {
    val nIds  = config.clientIds.size
    val n     = math.max(config.numClients, nIds)
    val res   = Array.tabulate(n) { i =>
      val id = if (i < nIds) config.clientIds(i) else 11 + i
      new InetSocketAddress(s"192.168.0.$id", clientPort)
    }
    res
  }

  private[this] val clientStatus = Array.fill[Status](clients.length)(Unknown)

  private[this] val serverConfig: UDP.Config = {
    val c = UDP.Config()
    c.localSocketAddress = ServerAddress
    c.build
  }
  private[this] val transmitter = UDP.Transmitter(serverConfig)

  private[this] val receiver: UDP.Receiver.Undirected = {
    val res = UDP.Receiver(transmitter.channel)
    res.action = received
    res
  }

  def start(): Unit = {
    receiver   .connect()
    transmitter.connect()
    if (config.dumpOSC) {
      receiver   .dump()
      transmitter.dump()
    }
  }

  def shutdown(): Unit = {
    log("Issuing shutdown...")
    var j = 0
    val cmd = osc.Message("/shutdown")
    while (j < clients.length) {
      try {
        transmitter.send(cmd, clients(j))
      } catch {
        case NonFatal(ex) =>
          ex.printStackTrace()
      }
      j += 1
    }
    Main.shutdown()
  }

  def reboot(): Unit = {
    log("Issuing reboot...")
    var j = 0
    val cmd = osc.Message("/reboot")
      while (j < clients.length) {
      try {
        transmitter.send(cmd, clients(j))
      } catch {
        case NonFatal(ex) =>
          ex.printStackTrace()
      }
      j += 1
    }
    Main.reboot()
  }

  private[this] var urnHH       = StrangeUrn(VideoSet       .all.toSet)
  private[this] var urnDP       = StrangeUrn(VideoSetsRattle.all.toSet)
  private[this] var isDP        = random.nextBoolean()
  private[this] var streakToGo  = 0

  private[this] var spawnCount  = 0

  private[this] var lastError   = "(no error)"

  private def spawnVideo(): Unit = {
    var success = false
    while (!success)
      try {
        spawnVideoImpl()
        success = true
      } catch {
        case NonFatal(ex) =>
          val boas  = new java.io.ByteArrayOutputStream
          val ps    = new java.io.PrintStream(boas)
          ex.printStackTrace(ps)
          ps.close()
          lastError = new String(boas.toByteArray, "UTF-8")
          ex.printStackTrace()
          Thread.sleep(1000)
          spawnVideo()
      }
  }

  private def spawnVideoImpl(): Unit = {
//    clientsReady = true
//    val cmdTest = osc.Message("/test")
    var j = 0
    while (j < clients.length) {
      if (clientStatus(j) != Unknown) {
        clientStatus(j) = Playing
      }
      j += 1
    }

    if (streakToGo == 0) {
      isDP  = !isDP
      streakToGo  = if (isDP) VideoSetsRattle.all.size else VideoSet.all.size
    }
    streakToGo -= 1

    val set = if (isDP) {
      val (_res, urnNew) = urnDP.choose()
      urnDP = urnNew
      _res
    } else {
      spawnCount += 1
      val _set = if (spawnCount % 2 == 0) {
        val (_res, urnNew) = urnHH.choose()
        urnHH = urnNew
        _res
      } else {
        VideoSetFragments
      }
      _set
    }
    val cmds = set.select()

    j = 0
    while (j < clients.length) {
      if (clientStatus(j) != Unknown) {
        val cmd   = cmds(j % cmds.size)
//        val vid   = vidFmt.format(vidId)
//        val cmd   = Play(file = vid, delay = 0f, start = 0f, duration = 30f, orientation = 0, fadeIn = 4f, fadeOut = 4f)
        try {
          transmitter.send(cmd, clients(j))
        } catch {
          case NonFatal(ex) =>
            ex.printStackTrace()
            clientStatus(j) = Idle
        }
      }
      j += 1
    }
  }

  private[this] var isFirstReady = true

  private[this] val numClientsC = clients.length // math.min(clients.length, config.numClients)

  private def received(p: Packet, addr: SocketAddress): Unit = {
    var clientIdx = -1
    var i = 0
    while (i < clients.length && clientIdx < 0) {
      if (clients(i) == addr) clientIdx = i
      i += 1
    }
    if (clientIdx < 0) {
      p match {
        case osc.Message("/forward", cmdM: String, args @ _*) =>
          val cmd = osc.Message(cmdM, args: _*)
          var j = 0
          while (j < clients.length) {
            try {
              transmitter.send(cmd, clients(j))
            } catch {
              case NonFatal(ex) =>
                ex.printStackTrace()
                clientStatus(j) = Idle
            }
            j += 1
          }

        case osc.Message("/forward-to", idx: Int, cmdM: String, args @ _*) =>
          val cmd = osc.Message(cmdM, args: _*)
          try {
            val c = clients(idx)
            transmitter.send(cmd, c)
          } catch {
            case NonFatal(ex) =>
              ex.printStackTrace()
          }

        case osc.Message("/query-error") =>
          val cmd = osc.Message("/last-error", lastError)
          try {
            transmitter.send(cmd, addr)
          } catch {
            case NonFatal(ex) =>
              ex.printStackTrace()
          }

        case _ =>
          Console.err.println(s"Unknown client $addr")
      }
    } else {
      p match {
        case osc.Message("/status", statusId: String) =>
          val status = Status(statusId)
          log(s"client[$clientIdx] = $status")
          clientStatus(clientIdx) = status
          if (/* !clientsReady && */ clientStatus.count(_ == Idle) == numClientsC) {
            if (isFirstReady) {
              log("All clients are ready!")
              isFirstReady = false
            }
            spawnVideo()
          }

        case _ =>
          Console.err.println(s"Unknown OSC message $p from $addr")
      }
    }
  }
}