package utils

import java.net.{NetworkInterface, InetAddress}

import scala.collection.JavaConversions._

object NetworkUtils {

  def getAllIps(): List[String] = {
    val rez = complexIp().map(_.getInterfaceAddresses.head.getAddress.getHostAddress()).map(_.toString)
    getIp() :: rez
  }

  def getIp(): String = {
    NetworkUtilsJava.getLocalHostLANAddress().getHostAddress()
  }

  private def complexIp(): List[NetworkInterface] = {
    NetworkInterface.getNetworkInterfaces().toList
  }
}
