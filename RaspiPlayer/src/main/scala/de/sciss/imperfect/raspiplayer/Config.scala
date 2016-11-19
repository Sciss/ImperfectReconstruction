/*
 *  Config.scala
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

import java.io.{FileInputStream, FileOutputStream}
import java.net.{Inet4Address, InetAddress}

import de.sciss.file._

object Config {
  /** Maps MAC addresses to IP addresses */
  val ipMap: Map[String, String] = Map(
    "b8:27:eb:71:d5:56" -> "192.168.0.11",
    "b8:27:eb:55:1b:32" -> "192.168.0.12",
    "b8:27:eb:76:1c:85" -> "192.168.0.13",
    "b8:27:eb:37:83:bc" -> "192.168.0.14",
    "b8:27:eb:42:00:49" -> "192.168.0.15",
    "b8:27:eb:72:d1:70" -> "192.168.0.16",
    "b8:27:eb:d9:a5:b9" -> "192.168.0.17",
    "b8:27:eb:c5:19:a6" -> "192.168.0.18",
    "b8:27:eb:36:2e:72" -> "192.168.0.19"
  )

  val controlIP = "192.168.0.11"

  // def main(args: Array[String]): Unit = checkIP()

  /** Verifies IP according to `ipMap` and
    * MAC address. If IP doesn't match, tries
    * to edit `/etc/dhcpcd.conf` and reboot.
    * This way, we can clone the Raspberry Pi
    * image, and each machine can configure
    * itself from the identical clone.
    */
  def checkIP(): String = {
    import sys.process._
    val macAddress  = Seq("cat", "/sys/class/net/eth0/address").!!.trim
    val ifConfig    = Seq("/sbin/ifconfig", "eth0").!!
    val ifConfigPat = "inet addr:"
    val i0          = ifConfig.indexOf(ifConfigPat)
    val i1          = if (i0 < 0) 0 else i0 + ifConfigPat.length
    val i2          = ifConfig.indexOf(" ", i1)
    if (i0 < 0 || i2 < 0) {
      Console.err.println("No assigned IP4 found in eth0!")
      InetAddress.getLocalHost.getHostName
    } else {
      val currentHost = ifConfig.substring(i1, i2)
      ipMap.get(macAddress).fold[String] {
        Console.err.println(s"Unknown MAC address: $macAddress - not trying to match IP.")
        currentHost
      } { desiredIP =>
        println(s"This computer has MAC address $macAddress and IP $currentHost")
        if (desiredIP != currentHost) {
          val confPath = "/etc/dhcpcd.conf"
          println(s"Designated IP is $desiredIP. Updating /etc/dhcpcd.conf...")
          val header = "interface eth0"
          Seq("cp", confPath, s"$confPath.BAK").!
          val init = io.Source.fromFile(file(confPath)).getLines().toList
            .takeWhile(ln => ln.trim() != header).mkString("\n")
          val tail =
            s"""$header
               |
               |static ip_address=$desiredIP/24
               |static routers=192.168.0.1
               |static domain_name_servers=192.168.0.1
               |""".stripMargin
          val contents = s"$init\n$tail"

          val fOut  = new FileOutputStream(confPath)
          fOut.write(contents.getBytes("UTF-8"))
          fOut.close()
          println("Rebooting...")
          Seq("sudo", "reboot", "now").!
        }
        currentHost
      }
    }
  }
}
final case class Config(isControl: Boolean, thisHost: String, clientPort: Int = 57120,
                        testVideo: File = userHome / "Videos" / "test.mp4", dumpOSC: Boolean = false)
