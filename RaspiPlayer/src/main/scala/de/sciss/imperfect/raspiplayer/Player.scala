/*
 *  Player.scala
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

import java.net.InetSocketAddress

import de.sciss.file._
import de.sciss.osc
import de.sciss.osc.{Packet, TCP, UDP}

import scala.util.control.NonFatal

final class Player(config: Config) {
  private[this] val received: (Packet => Unit) = {
    case osc.Message("/status") =>
    case osc.Message("/test") =>
      import sys.process._
      Seq("omxplayer", "--loop", config.testVideo.path).run()

    case p =>
      Console.err.println(s"Unknown OSC message $p from control")
  }

  private[this] val client = {
    val res = UDP.Client(new InetSocketAddress("192.168.0.11", 57110))
    res.action = received
    res
  }

  private[this] var status: Status = Idle

  def start(): Unit = {
    new Thread {
      override def run(): Unit = {
        while (!client.isConnected)
        try {
          println("Trying to connect to server...")
          client.connect()
        } catch {
          case NonFatal(_) =>
            Thread.sleep(1000)
        }
      }
    }
  }
}
