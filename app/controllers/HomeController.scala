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
  def index = Action {

     implicit request =>

       val apps = ClevercloudApi.all_apps()
       val instances = ClevercloudApi.all_instances()

       val ip = ClevercloudApi.getCurrentInstanceIp()
       val cluster_ip = ClevercloudApi.getRunningInstanceIp()

    Ok(views.html.index(ip, cluster_ip, apps, instances))
  }

    val KEY = ConsumerKey(ClevercloudApi.consumerKey, ClevercloudApi.consumerSecret)

    val oauth = OAuth(ServiceInfo(
      "https://api.clever-cloud.com/v2/oauth/request_token_query",
      "https://api.clever-cloud.com/v2/oauth/access_token_query",
      "https://api.clever-cloud.com/v2/oauth/authorize",
      KEY),
      true)

    def sessionTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
      for {
        token <- request.session.get("token")
        secret <- request.session.get("secret")
      } yield {
        RequestToken(token, secret)
      }
    }

    def authenticate = Action { request =>
      request.getQueryString("oauth_verifier").map { verifier =>
        val tokenPair = sessionTokenPair(request).get
        // We got the verifier; now get the access token, store it and back to index
        oauth.retrieveAccessToken(tokenPair, verifier) match {
          case Right(t) => {
            // We received the authorized tokens in the OAuth object - store it before we proceed
            Redirect(routes.HomeController.index).withSession("token" -> t.token, "secret" -> t.secret)
          }
          case Left(e) => throw e
        }
      }.getOrElse(
        oauth.retrieveRequestToken("http://localhost:9000/auth") match {
          case Right(t) => {
            // We received the unauthorized tokens in the OAuth object - store it before we proceed
            Redirect(oauth.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
          }
          case Left(e) => throw e
        })
    }

    def timeline = Action.async { implicit request =>
      sessionTokenPair match {
        case Some(credentials) => {
          WS.url("https://api.twitter.com/1.1/statuses/home_timeline.json")
            .sign(OAuthCalculator(KEY, credentials))
            .get
            .map(result => Ok(result.json))
        }
        case _ => Future.successful(Redirect(routes.HomeController.authenticate))
      }
    }


}
