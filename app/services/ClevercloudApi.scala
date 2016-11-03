package services

import com.google.inject.{Inject, Singleton}
import io.swagger.client.model.AppInstance
import play.api.{Logger, Play, Configuration}
import play.api.Play.current
import utils.NetworkUtils

import io.swagger.client._
import io.swagger.client.api.DefaultApi

import scala.collection.JavaConverters._

@Singleton
class ClevercloudApi @Inject() (configuration: Configuration) {

  private val instance_nb = configuration.getString("clevercloud.instance.number").getOrElse("not_set")
  private val instance_id = configuration.getString("clevercloud.instance.id").getOrElse("not_set")

  private val app_test_cluster = configuration.getString("clevercloud.app_id.test_cluster").getOrElse("not_set")
  private val particeep = configuration.getString("clevercloud.org_id.particeep").getOrElse("not_set")

  private val base_url = configuration.getString("clevercloud.api.base_url").getOrElse("https://api.clever-cloud.com/v2/")
  private val consumerKey = configuration.getString("clevercloud.api.consumerKey").getOrElse("8IitRr6CvosYtKE2dYFHJbpn83keRf")
  private val consumerSecret = configuration.getString("clevercloud.api.consumerSecret").getOrElse("wvJeEAAki8kHQrONgkjAzscF3LUMHr")
  private val oauth_token = configuration.getString("clevercloud.api.oauth_token").getOrElse("99486087b7a24132ae57df260ac79865")
  private val oauth_verifier = configuration.getString("clevercloud.api.oauth_verifier").getOrElse("b66de96a21364822919d1cfaab5d1a8e")

  final val api: DefaultApi = new DefaultApi()
  final val apiClient: CleverApiClient = buildClient()

  private def buildClient(): CleverApiClient = {
    val apiClient = new CleverApiClient(consumerKey, consumerSecret, oauth_token, oauth_verifier)
    api.setApiClient(apiClient)
    apiClient
  }

  def all_apps(): List[String] = {
    api.getOrganisationsIdApplications(particeep).asScala.toList.map(_.getName)
  }

  def all_instances(): List[AppInstance] = {
    api.getOrganisationsIdApplicationsAppIdInstances(particeep, app_test_cluster).asScala.toList
  }

  def getCurrentInstanceIp(): (String, Int) = {
    all_instances
      .filter(_.getId == instance_id)
      .map(i => (i.getIp, i.getAppPort.intValue()))
      .headOption
      .getOrElse((NetworkUtils.getIp(), NetworkUtils.getPort()))
  }

  def getRunningInstanceIp(): List[(String, Int)] = {
    all_instances
      .filter(_.getState == "UP")
      .map(i => (i.getIp, i.getAppPort.intValue()))

    //List(("192.168.1.16", 2552), ("192.168.1.16", 2551))
    //List(("127.0.0.1", 2552), ("127.0.0.1", 2551))
  }

  def getOtherInstanceIp(): List[(String, Int)] = {
    getRunningInstanceIp().filter(_ != getCurrentInstanceIp())
  }

  def isSeedNode(): Boolean = {
    val from_property = "y".equals(System.getProperty("seed"))

    val from_config = configuration.getBoolean("application.cluster.is.seed").getOrElse(false)

    val from_cc = all_instances
      .filter(app => starting_status.contains(app.getState()))
      .sortBy(_.getDeployNumber)
      .headOption
      .map(_.getId == instance_id)
      .getOrElse(from_property)

    Logger.warn(s"is seed : $from_property - $from_config - $from_cc")
    from_property || from_config || from_cc
  }

  private def instanceUpBeforeMeExist(): Boolean = {
    !all_instances
      .filter(_.getState() == "UP")
      .filter(_.getDeployNumber() < instance_nb.toInt)
      .isEmpty
  }

  private def starting_status = List("BOOTING", "STARTING", "DEPLOYING", "READY", "UP")
  private def stoping_status = List("STOPPING", "DELETED", "GHOST")
  private def status = starting_status ++ stoping_status
}
