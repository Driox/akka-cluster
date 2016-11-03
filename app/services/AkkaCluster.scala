package services

import actor.BroadcastActor
import actor.BroadcastActor.Message
import akka.actor.{ActorRef, Address, Props, ActorSystem}
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
class AkkaCluster @Inject() (clevercloudApi: ClevercloudApi, configuration: Configuration, system: ActorSystem) {

  Logger.info(s"[AkkaCluster] starting")
  println(s">>> [AkkaCluster] starting")
  init_from_config()

  private def create_system(): ActorSystem = {
    Logger.info(s"[AkkaCluster] creating system")

    //val seeds = clevercloudApi.getOtherInstanceIp() map (ip => s"akka.tcp://akka-cc@${ip._1}:${ip._2}")
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

  private def join_cluster(system: ActorSystem, is_seed: Boolean) = {
    val cluster = Cluster(system)
    if (is_seed) {
      val ip = clevercloudApi.getCurrentInstanceIp()
      Logger.info(s"[AkkaCluster] join seed node = $ip")
      cluster.join(Address("akka.tcp", system.name, ip._1, ip._2))
    } else {
      clevercloudApi.getOtherInstanceIp().headOption.map { ip =>
        Logger.info(s"[AkkaCluster] cluster node = $ip")
        cluster.join(Address("akka.tcp", system.name, ip._1, ip._2))
      }
    }
  }

  def init_from_config() = {

    val system: ActorSystem = ActorSystem.create("akka-cc", ConfigFactory.load().getConfig("akka-cc"))

    //val master:ActorRef = system.actorFor("akka://master@your-master-host-name:your-master-port/user/master")

    val broadcaster = system.actorOf(Props[BroadcastActor], name = "broadcast")
    dispatch_msg(broadcaster)
  }

  def init() = {
    val is_seed = clevercloudApi.isSeedNode()
    Logger.info(s"[AkkaCluster] init - is_seed : $is_seed")
    val system: ActorSystem = create_system()

    Logger.info(s"[AkkaCluster] join cluster")
    join_cluster(system, is_seed)

    Logger.info(s"[AkkaCluster] add brodcaster actor")
    val broadcaster = system.actorOf(Props[BroadcastActor], name = "broadcast")

    if (is_seed) {
      dispatch_msg(broadcaster)
    }
  }

  def dispatch_msg(broadcaster: ActorRef) = {
    Logger.info(s"[AkkaCluster] start scheduler")
    implicit val executor = system.dispatcher
    system.scheduler.schedule(0 seconds, 5 seconds) {
      val words = Random.shuffle(
        List("peter", "piper", "picked", "a", "peck", "of", "pickled", "pepper")
      )
      Logger.warn(s"[AkkaCluster] start dispatch word : $words")
      broadcaster ! Message(words mkString " ")
    }
  }
}
