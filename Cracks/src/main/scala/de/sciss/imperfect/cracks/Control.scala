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

package de.sciss.imperfect.cracks

import java.net.{InetSocketAddress, SocketAddress}

import de.sciss.osc
import de.sciss.osc.{Packet, UDP}

import scala.util.control.NonFatal

final class Control(config: Config) {
  import config._

//  private[this] implicit val random: Random = new Random()

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
            // spawnVideo()
          }

        case _ =>
          Console.err.println(s"Unknown OSC message $p from $addr")
      }
    }
  }
}