package utils

import java.net.{NetworkInterface, InetAddress}

import play.api.Logger

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

object NetworkUtils {

  def getAllIps(): List[String] = {
    val rez = complexIp().map(_.getInterfaceAddresses.head.getAddress.getHostAddress())
    getIp() :: rez
  }

  def getIp(): String = {
    NetworkUtilsJava.getLocalHostLANAddress().getHostAddress()
  }

  def getPort(): Int = {
    try {
      Integer.valueOf(System.getProperty("currentPort"))
    } catch {
      case NonFatal(e) => {
        Logger.error("Can't parse port from System.getProperty", e)
        80
      }
    }
  }

  private def complexIp(): List[NetworkInterface] = {
    NetworkInterface.getNetworkInterfaces().toList
  }
}
