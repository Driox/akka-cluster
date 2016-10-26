package services

import actor.BroadcastActor
import actor.BroadcastActor.Message
import akka.actor.{Address, Props, ActorSystem}
import akka.cluster.Cluster
import com.typesafe.config.{ConfigValueFactory, ConfigFactory}
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import utils.NetworkUtils

import scala.util.Random
import scala.concurrent.duration._
import com.google.inject.{Inject, Singleton, AbstractModule}
import com.google.inject.name.Names

import play.api.{Configuration, Environment}

import collection.JavaConverters._

@Singleton
class AkkaCluster @Inject() (clevercloudApi: ClevercloudApi, configuration: Configuration) {

  Logger.info(s"[AkkaCluster] starting")
  println(s">>> [AkkaCluster] starting")
  init()

  private def create_system(): ActorSystem = {
    Logger.info(s"[AkkaCluster] creating system")

    val seeds = clevercloudApi.getOtherInstanceIp() map (ip => s"akka.tcp://akka-cc@${ip._1}:${ip._2}")
    //val seeds_config: java.lang.Iterable[String] = seeds.toIterable.asJava
    val currentIp = clevercloudApi.getCurrentInstanceIp()

    Logger.info(s"[AkkaCluster] currentIp = $currentIp")

    val overrideConfig =
      ConfigFactory.empty()
        .withValue("akka.actor.provider", ConfigValueFactory.fromAnyRef("cluster"))
        .withValue("akka.remote.log-remote-lifecycle-events", ConfigValueFactory.fromAnyRef("off"))
        .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(currentIp._1))
        .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(currentIp._2))
    //.withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seeds_config))

    //        .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef("test-cluster.particeep.com"))
    //        .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(80))
    //        .withValue("akka.remote.netty.tcp.bind-hostname", ConfigValueFactory.fromAnyRef(NetworkUtils.getIp()))
    //        .withValue("akka.remote.netty.tcp.bind-port", ConfigValueFactory.fromAnyRef(cluster_port))

    val system = ActorSystem("akka-cc", overrideConfig withFallback ConfigFactory.load())

    system
  }

  private def join_cluster(system: ActorSystem) = {
    val cluster = Cluster(system)
    if (clevercloudApi.isSeedNode()) {
      val ip = clevercloudApi.getCurrentInstanceIp()
      cluster.join(Address("akka.tcp", system.name, ip._1, ip._2))
    } else {
      clevercloudApi.getOtherInstanceIp().headOption.map { ip =>
        cluster.join(Address("akka.tcp", system.name, ip._1, ip._2))
      }
    }
  }

  def init() = {
    Logger.info(s"[AkkaCluster] init")
    val system: ActorSystem = create_system()

    Logger.info(s"[AkkaCluster] join cluster")
    join_cluster(system)

    Logger.info(s"[AkkaCluster] add brodcaster actor")
    val broadcaster = system.actorOf(Props[BroadcastActor], name = "broadcast")

    Logger.info(s"[AkkaCluster] start scheduler")
    implicit val executor = system.dispatcher
    system.scheduler.schedule(0 seconds, 5 seconds) {
      val words = Random.shuffle(
        List("peter", "piper", "picked", "a", "peck", "of", "pickled", "pepper")
      )
      broadcaster ! Message(words mkString " ")
    }
  }
}
