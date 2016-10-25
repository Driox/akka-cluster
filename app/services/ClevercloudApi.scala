package services

import io.swagger.client.model.AppInstance
import play.api.Play
import play.api.Play.current
import utils.NetworkUtils

import io.swagger.client._
import io.swagger.client.api.DefaultApi

import scala.collection.JavaConverters._


object ClevercloudApi {

  private val instance_nb = Play.configuration.getString("clevercloud.instance.number").getOrElse("not_set")
  private val instance_id = Play.configuration.getString("clevercloud.instance.id").getOrElse("not_set")

  private val app_test_cluster = Play.configuration.getString("clevercloud.app_id.test_cluster").getOrElse("not_set")
  private val particeep = Play.configuration.getString("clevercloud.org_id.particeep").getOrElse("not_set")

  private val base_url = Play.configuration.getString("clevercloud.api.base_url").getOrElse("https://api.clever-cloud.com/v2/")
  private val consumerKey = Play.configuration.getString("clevercloud.api.consumerKey").getOrElse("8IitRr6CvosYtKE2dYFHJbpn83keRf")
  private val consumerSecret = Play.configuration.getString("clevercloud.api.consumerSecret").getOrElse("wvJeEAAki8kHQrONgkjAzscF3LUMHr")
  private val oauth_token = Play.configuration.getString("clevercloud.api.oauth_token").getOrElse("99486087b7a24132ae57df260ac79865")
  private val oauth_verifier = Play.configuration.getString("clevercloud.api.oauth_verifier").getOrElse("b66de96a21364822919d1cfaab5d1a8e")

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

  def getCurrentInstanceIp(): String = {
     api.getOrganisationsIdApplicationsAppIdInstances(particeep, app_test_cluster).asScala.toList
      .filter(_.getId == instance_id)
      .map( i => (i.getIp, i.getAppPort.intValue()))
      .map(i => s"${i._1}:${i._2}")
      .headOption
      .getOrElse(NetworkUtils.getIp())
  }

  def getRunningInstanceIp(): List[(String, Int)] = {
    val instances = api.getOrganisationsIdApplicationsAppIdInstances(particeep, app_test_cluster).asScala.toList
    instances
      .filter(_.getState == "UP")
      .map( i => (i.getIp, i.getAppPort.intValue()))
  }

  def getOtherInstanceIp(): List[(String, Int)] = {
    getRunningInstanceIp().filter(s => s._1 != getCurrentInstanceIp())
  }
}
