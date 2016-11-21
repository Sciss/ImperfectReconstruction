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

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.util.Random
import scala.util.control.NonFatal

final class Control(config: Config) {
  import config._

  private[this] val random  = new Random()

  private[this] val clients = {
    // /etc/dhcpcd.conf
    val res = Array.tabulate[InetSocketAddress](8) { i =>
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
  }

  def quit(): Unit = {
    log("Quitting...")
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

//  private[this] var clientsReady = false

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

    val vidIds: Vec[Int] = random.shuffle[Int, Vec](1 to 8)
    val vidFmt = random.nextInt(3) match {
      case 0 => "site/site%d.mp4"
      case 1 => "notebook/notebook%d.mp4"
      case 2 => "precious/precious%daf.mp4"
    }

    j = 0
    while (j < clients.length) {
      if (clientStatus(j) != Unknown) {
        val vidId = vidIds(j % vidIds.size)
        val vid   = vidFmt.format(vidId)
        val cmd   = Play(file = vid, start = 0f, duration = 30f, orientation = 0, fadeIn = 4f, fadeOut = 4f)
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