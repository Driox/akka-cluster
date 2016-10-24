package services

import java.io.{InputStreamReader, BufferedReader}
import java.nio.charset.Charset

import io.swagger.client.model.AppInstance
import org.glassfish.jersey.client.oauth1._
import play.api.Play
import play.api.Play.current
import play.api.libs.oauth._
import play.api.libs.ws._
import play.api.mvc.{Action, RequestHeader}
import utils.NetworkUtils

import io.swagger.client._
import io.swagger.client.api.DefaultApi

import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import scala.collection.JavaConverters._


object ClevercloudApi {

  val app_ip = Play.configuration.getString("clevercloud.app_id").getOrElse("not_set")
  val org_ip = Play.configuration.getString("clevercloud.org_id").getOrElse("not_set")
  val base_url = "https://api.clever-cloud.com/v2/"
  val consumerKey = "8IitRr6CvosYtKE2dYFHJbpn83keRf"
  val consumerSecret = "wvJeEAAki8kHQrONgkjAzscF3LUMHr"
  val oauth_token = "99486087b7a24132ae57df260ac79865"
  val oauth_verifier = "b66de96a21364822919d1cfaab5d1a8e"

  val particeep = "orga_5c2880c5-0c9e-4b5a-acab-085ed2f8f950"
  val app_api_test = "app_be0fdb37-7ea8-441c-b733-d717f41caa77"
  val app_test_cluster = "app_b621d434-a282-402b-8e7a-20bd77f0733c"

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
//      .map{i =>
//        s"""
//           |id : ${i.getAppId}
//           |ip : ${i.getIp}:${i.getAppPort}
//           |flavor : ${i.getFlavor}
//           |state : ${i.getState}
//           |commit : ${i.getCommit}
//           |deploy # : ${i.getDeployNumber}
//         """.stripMargin
//      }
  }

  def getCurrentInstanceIp(): String = NetworkUtils.getIp()

  def getRunningInstanceIp(): List[String] = {
    val instances = api.getOrganisationsIdApplicationsAppIdInstances(particeep, app_api_test).asScala.toList
    instances
      .filter(_.getState == "UP")
      .map( zz => s"${zz.getIp}:${zz.getAppPort}")
  }
}


object ClevercloudApi_Test {
    def test2() = {
    var api = new DefaultApi()
    var apiClient = new CleverApiClient(ClevercloudApi.consumerKey, ClevercloudApi.consumerSecret, ClevercloudApi.oauth_token, ClevercloudApi.oauth_verifier)
    api.setApiClient(apiClient)

    try {
      println(api.getOrganisationsIdApplications(ClevercloudApi.particeep))
    } catch {
      case e: ApiException => println(e)
    }
  }


  def getAccessToken() = {
    val client: Client = ClientBuilder.newBuilder().build()
    val consumerCredentials = new ConsumerCredentials(ClevercloudApi.consumerKey, ClevercloudApi.consumerSecret)
    val authFlow: OAuth1AuthorizationFlow = OAuth1ClientSupport.builder(consumerCredentials)
      .authorizationFlow("https://api.clever-cloud.com/v2/oauth/request_token_query",
        "https://api.clever-cloud.com/v2/oauth/access_token_query",
        "https://api.clever-cloud.com/v2/oauth/authorize")
      .enableLogging()
      .client(client)
      .callbackUri("https://test-cluster.particeep.com")
      .build()
    val authorizationUri: String = authFlow.start()
    println("Enter the following URI into a web browser and authorize me:")
    println(authorizationUri)
    val IN = new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF-8")))
    println("Enter the authorization code: ")
        var in:String = null

        try {
            in = IN.readLine()
        } catch {
          case e:Exception => e.printStackTrace()
        }
    println(s"token entered : $in")
    val accessToken:AccessToken = authFlow.finish(in)
    println("Your token : " + accessToken.getToken() + "\nYour token secret : " + accessToken.getAccessTokenSecret())

  }
}
