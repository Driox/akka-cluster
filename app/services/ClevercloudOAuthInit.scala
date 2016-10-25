package services

import java.io.{InputStreamReader, BufferedReader}
import java.nio.charset.Charset

import org.glassfish.jersey.client.oauth1._
import play.api.Play
import play.api.Play.current

import io.swagger.client._
import io.swagger.client.api.DefaultApi

import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder


object ClevercloudApi_Test {

  private val consumerKey = Play.configuration.getString("clevercloud.api.consumerKey").getOrElse("8IitRr6CvosYtKE2dYFHJbpn83keRf")
  private val consumerSecret = Play.configuration.getString("clevercloud.api.consumerSecret").getOrElse("wvJeEAAki8kHQrONgkjAzscF3LUMHr")
  private val oauth_token = Play.configuration.getString("clevercloud.api.oauth_token").getOrElse("99486087b7a24132ae57df260ac79865")
  private val oauth_verifier = Play.configuration.getString("clevercloud.api.oauth_verifier").getOrElse("b66de96a21364822919d1cfaab5d1a8e")

  private val particeep = Play.configuration.getString("clevercloud.org_id.particeep").getOrElse("not_set")

  
  def test2() = {
    var api = new DefaultApi()
    var apiClient = new CleverApiClient(consumerKey, consumerSecret, oauth_token, oauth_verifier)
    api.setApiClient(apiClient)

    try {
      println(api.getOrganisationsIdApplications(particeep))
    } catch {
      case e: ApiException => println(e)
    }
  }


  def getAccessToken() = {
    val client: Client = ClientBuilder.newBuilder().build()
    val consumerCredentials = new ConsumerCredentials(consumerKey, consumerSecret)
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
