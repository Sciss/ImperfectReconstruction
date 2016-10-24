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

import java.net.{InetAddress, InetSocketAddress, SocketAddress}

import de.sciss.osc
import de.sciss.osc.{Packet, TCP, UDP}

import scala.util.Random

final class Control(config: Config) {
  import config._

  private[this] val random  = new Random()

  private[this] val clients = {
    // /etc/dhcpcd.conf
    val res = new Array[InetSocketAddress](3)
    res(0) = new InetSocketAddress("192.168.0.11", clientPort)
    res(1) = new InetSocketAddress("192.168.0.12", clientPort)
    res(2) = new InetSocketAddress("192.168.0.13", clientPort)
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
    transmitter.connect()
    receiver   .connect()
  }

  private[this] var clientsReady = false

  private[this] def received(p: Packet, addr: SocketAddress): Unit = {
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
          if (!clientsReady && clientStatus.forall(_ == Idle)) {
            println("All clients are ready!")
            clientsReady = true
            val cmdTest = osc.Message("/test")
            var j = 0
            while (j < clients.length) {
              transmitter.send(cmdTest, clients(j))
              j += 1
            }
          }

        case _ =>
          Console.err.println(s"Unknown OSC message $p from $addr")
      }
    }
  }
}