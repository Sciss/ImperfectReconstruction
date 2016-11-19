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
import de.sciss.osc.{Packet, UDP}

import scala.util.Try
import scala.util.control.NonFatal

final class Player(config: Config) {
  private[this] var status: Status = Idle

  private[this] val client = {
    val c   = UDP.Config()
    c.localSocketAddress = new InetSocketAddress(config.thisHost /* InetAddress.getLocalHost() */, config.clientPort)
    val res = UDP.Client(new InetSocketAddress("192.168.0.11", 57110), c)
    res.action = received
    res
  }

  private[this] def sendStatus(): Unit = client ! osc.Message("/status", status.id)

  private[this] def received(p: Packet): Unit = p match {
    case osc.Message("/status") =>
      sendStatus()

    case osc.Message("/test") =>
      import sys.process._
      Seq("omxplayer", "--loop", config.testVideo.path).run()

    case osc.Message("/shell", cmd @ _*) =>
      val cmdS = cmd.map(_.toString)
      println("Executing shell command:")
      println(cmdS.mkString(" "))
      import sys.process._
      val result = Try(cmdS.!!).toOption.getOrElse("ERROR")
      client ! osc.Message("/shell_reply", result)

    case _ =>
      Console.err.println(s"Unknown OSC message $p from control")
  }

  def start(): Unit = {
    new Thread {
      override def run(): Unit = {
        while (!client.isConnected)
        try {
          log("Trying to connect to server...")
          client.connect()
          log("Client connected.")
          sendStatus()
        } catch {
          case NonFatal(_) =>
            Thread.sleep(1000)
        }
      }
    } .start()
  }
}
