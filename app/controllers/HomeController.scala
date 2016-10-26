package controllers

import java.net.InetAddress
import javax.inject._
import play.api.Play.current
import play.api._
import play.api.libs.json.Json
import play.api.libs.oauth._
import play.api.libs.ws._
import play.api.mvc._
import services.ClevercloudApi
import utils.NetworkUtils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() extends Controller {

  def ping = Action {
    println(s"ping done")
    Ok(s"OK")
  }

  def ping_huge() = Action {
    println(s"ping huge done")
    val huge_list = List.fill(10000)(Random.nextString(20))
    val json = Json.toJson(huge_list)
    Ok(json)
  }

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action { implicit request =>

    val apps = ClevercloudApi.all_apps()
    val instances = ClevercloudApi.all_instances()

    val internal_ip = NetworkUtils.getIp()
    val ip = ClevercloudApi.getCurrentInstanceIp()
    val cluster_ip = ClevercloudApi.getRunningInstanceIp().map(s => s"${s._1}:${s._2}")

    Ok(views.html.index(internal_ip, s"${ip._1}:${ip._2}", cluster_ip, apps, instances))
  }

  def test(path: String) = Action.async { implicit request =>
    val current_ip = ClevercloudApi.getCurrentInstanceIp()
    val other_ips = ClevercloudApi.getOtherInstanceIp()
    val other_ip = other_ips.headOption

    Logger.warn(
      s"""
         |current_ip = $current_ip
         |other_ips = $other_ips
       """.stripMargin
    )

    other_ip.map(s => s"http://${s._1}:${s._2}/$path")
      .map { url =>
        Logger.warn(s"test sur url $url form ip = $current_ip")
        requestWithTiming(url)
      }.getOrElse(Future.successful(Ok("no data")))
  }

  private def requestWithTiming(url: String) = {
    val start_at = System.currentTimeMillis()
    WS.url(url).get().map { response =>
      val end_at = System.currentTimeMillis()
      val body = response.body
      Logger.warn(s"response in ${(end_at - start_at)} ms : $response with body $body")
      Ok(response.body)
    } recover {
      case e: Exception => {
        Logger.error(s"Error while sending test request on $url", e)
        Ok(s"Exception : $e")
      }
    }
  }
}
