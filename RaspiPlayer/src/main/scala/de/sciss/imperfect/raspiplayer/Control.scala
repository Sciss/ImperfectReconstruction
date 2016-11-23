/*
 *  Control.scala
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

package de.sciss.imperfect.raspiplayer

import java.net.{InetSocketAddress, SocketAddress}

import de.sciss.osc
import de.sciss.osc.{Packet, UDP}

import scala.util.Random
import scala.util.control.NonFatal

final class Control(config: Config) {
  import config._

  private[this] implicit val random: Random = new Random()

  private[this] val clients: Array[InetSocketAddress] = {
    // /etc/dhcpcd.conf
    val res = Array.tabulate(8) { i =>
      new InetSocketAddress(s"192.168.0.${11 + i}", clientPort)
    }
    res
  }

  private[this] val clientStatus = Array.fill[Status](clients.length)(Unknown)

  private[this] val serverConfig = {
    val c = UDP.Config()
    c.localSocketAddress = ServerAddress
    c.build
  }
  private[this] val transmitter = UDP.Transmitter(serverConfig)
  private[this] val receiver    = {
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

  private[this] var urn = StrangeUrn(VideoSet.all.toSet)
  private[this] var spawnCount = 0

  private def spawnVideo(): Unit = {
//    clientsReady = true
//    val cmdTest = osc.Message("/test")
    var j = 0
    while (j < clients.length) {
      if (clientStatus(j) != Unknown) {
        clientStatus(j) = Playing
      }
      j += 1
    }

//    val vidIds: Vec[Int] = random.shuffle[Int, Vec](1 to 8)
//    val vidFmt = random.nextInt(3) match {
//      case 0 => "site/site%d.mp4"
//      case 1 => "notebook/notebook%d.mp4"
//      case 2 => "precious/precious%daf.mp4"
//    }
//    log(s"spawnVideo - vidFmt $vidFmt; ids ${vidIds.mkString(", ")}")
    spawnCount += 1
    val set = if (spawnCount % 2 == 0) {
      val (_set, urnNew) = urn.choose()
      urn = urnNew
      _set
    } else {
      VideoSetFragments
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

  private[this] val numClientsC = math.min(clients.length, config.numClients)

  private def received(p: Packet, addr: SocketAddress): Unit = {
    var clientIdx = -1
    var i = 0
    while (i < clients.length && clientIdx < 0) {
      if (clients(i) == addr) clientIdx = i
      i += 1
    }
    if (clientIdx < 0) {
      Console.err.println(s"Unknown client $addr")
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