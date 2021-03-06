package modules

import play.api.ApplicationLoader
import play.api.Configuration
import play.api.inject.guice._
import play.api.ApplicationLoader.Context
import com.typesafe.config.{ConfigValueFactory, ConfigFactory}
import play.api.Logger
import services.ClevercloudApi
import collection.JavaConverters._

class CustomApplicationLoader extends GuiceApplicationLoader {

  val system_name = "application"

  override protected def builder(context: Context): GuiceApplicationBuilder = {
    Logger.info("[CustomApplicationLoader] start builder")
    val builder = initialBuilder.in(context.environment).overrides(overrides(context): _*)

    //val prodConf = loadCustomConfig(context.initialConfiguration)
    //builder.loadConfig(prodConf ++ context.initialConfiguration)

    builder.loadConfig(context.initialConfiguration)
  }

  private def loadCustomConfig(init: Configuration): Configuration = {
    Logger.info("[CustomApplicationLoader] start loading custom config")
    val clevercloudApi = new ClevercloudApi(init)

    val seeds = loadSeedNodes(init, clevercloudApi)
    val currentIp = clevercloudApi.getCurrentInstanceIp()
    val cluster_port = load_cluster_port(init)

    Logger.info(
      s"""
         |overload config with dynamic info
         |currentIp : $currentIp
         |cluster_port : $cluster_port
         |seeds : ${seeds.mkString("\n")}
       """.stripMargin
    )

    val seeds_config: java.lang.Iterable[String] = seeds.toIterable.asJava

    val overrideConfig = ConfigFactory.empty()
      .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(currentIp._1))
      .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(currentIp._2))
      .withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seeds_config))

    Logger.info(s"[CustomApplicationLoader] new config $overrideConfig")

    Configuration(overrideConfig)
  }

  private def loadSeedNodes(init: Configuration, clevercloudApi: ClevercloudApi): List[String] = {

    val seed_ip = clevercloudApi
      .getSeedRunningInstanceIp()
      .headOption
      .map(ip => s"akka.tcp://$system_name@${ip._1}:${ip._2}")
    val node_ip = clevercloudApi
      .getNodeRunningInstanceIp()
      .headOption
      .map(ip => s"akka.tcp://$system_name@${ip._1}:${ip._2}")

    val rez = List(seed_ip).flatten ++ List(node_ip).flatten
    rez
  }

  private def load_cluster_port(configuration: Configuration) = configuration.getInt("application.cluster.port").getOrElse(2551)
}
