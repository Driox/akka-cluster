package controllers

import java.net.InetAddress
import javax.inject._
import play.api.Play.current
import play.api._
import play.api.libs.oauth._
import play.api.libs.ws._
import play.api.mvc._
import services.ClevercloudApi
import utils.NetworkUtils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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

    Ok(views.html.index(internal_ip, ip, cluster_ip, apps, instances))
  }

  def test = Action.async { implicit request =>
    val ip = request.queryString.get("ip").flatMap(_.headOption).getOrElse("")
    val port = request.queryString.get("port").flatMap(_.headOption).getOrElse("")
    val url = s"http://$ip:$port/ping"
    Logger.warn(s"test sur url $url form ip = ${ClevercloudApi.getCurrentInstanceIp()}")
    WS.url(url).get().map{ response =>
      Logger.warn(s"response : $response")
      Ok(response.body)
    } recover {
      case e:Exception => {
        Logger.error(s"Error while sending test request on $url", e)
        Ok(s"Exception : $e")
      }
    }

  }

  def test2 = Action.async { implicit request =>
    val current_ip = ClevercloudApi.getCurrentInstanceIp()
    val all_ip = ClevercloudApi.getRunningInstanceIp()
    val other_ip = all_ip.filter(s => s._1 != current_ip).headOption

    other_ip.map(s =>  s"http://${s._1}:${s._2}/ping")
      .map{ url =>
      Logger.warn(s"test sur url $url form ip = $current_ip")
      WS.url(url).get().map{ response =>
        Logger.warn(s"response : $response")
        Ok(response.body)
      } recover {
        case e:Exception => {
          Logger.error(s"Error while sending test request on $url", e)
          Ok(s"Exception : $e")
        }
      }
    }.getOrElse(Future.successful(Ok("no data")))
  }
}
