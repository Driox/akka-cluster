package utils

import java.net.InetAddress

object NetworkUtils {

  def getIp(): String = {
    val thisIp: InetAddress = InetAddress.getLocalHost()
    thisIp.getHostAddress()
  }
}
