package utils

import java.net.{NetworkInterface, InetAddress}

import scala.collection.JavaConversions._

object NetworkUtils {

  def getIps(): List[String] = {
    val rez = complexIp().map(_.getInterfaceAddresses.head.getAddress.getHostAddress()).map(_.toString)
    basicIp() :: rez
  }

  def getIp(): String = {
    basicIp()
  }

  private def basicIp():String = {
    val thisIp: InetAddress = InetAddress.getLocalHost()
    thisIp.getHostAddress()
  }

  def complexIp():List[NetworkInterface] = {
    val interfaces = NetworkInterface.getNetworkInterfaces().toList
    interfaces.foreach{ i =>
      println(
        s"""
           |name : ${i.getName}
           |inet : ${i.getInetAddresses}
           |adr  : ${i.getInterfaceAddresses}
           |hard : ${i.getHardwareAddress}
         """.stripMargin)
    }

    interfaces
  }
}
