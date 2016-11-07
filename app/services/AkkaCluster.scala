package services

import actor.{CounterSharding, Counter, BroadcastActor}
import actor.BroadcastActor.Message
import akka.actor.{ActorRef, Address, Props, ActorSystem}
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterShardingSettings, ClusterSharding}
import com.typesafe.config.{ConfigValueFactory, ConfigFactory}
import play.api.{Play, Logger, Configuration, Environment}
import play.api.inject.ApplicationLifecycle
import utils.NetworkUtils

import scala.util.Random
import scala.concurrent.duration._
import com.google.inject.{Inject, Singleton, AbstractModule}
import com.google.inject.name.Names

import collection.JavaConverters._

@Singleton
class AkkaCluster @Inject() (clevercloudApi: ClevercloudApi, configuration: Configuration, system: ActorSystem) {

  Logger.info(s"[AkkaCluster] starting")
  println(s">>> [AkkaCluster] starting")

  val system_cc: ActorSystem = init()

  def init() = {
    val is_seed = clevercloudApi.isSeedNode()
    Logger.info(s"[AkkaCluster] init - is_seed : $is_seed")

    if (is_seed) {
      init_from_config()
    } else {
      init_dyn()
    }
  }

  def init_from_config(): ActorSystem = {

    val system_cc = create_system() // ActorSystem.create("akka-cc", ConfigFactory.load().getConfig("akka-cc"))

    //val master:ActorRef = system.actorFor("akka://master@your-master-host-name:your-master-port/user/master")

    val broadcaster = system_cc.actorOf(Props[BroadcastActor], name = "broadcast")
    dispatch_msg(broadcaster)
    CounterSharding.launch_sharding(system_cc)

    system_cc
  }

  def init_dyn(): ActorSystem = {
    val system_cc = create_system() // ActorSystem.create("akka-cc", ConfigFactory.load().getConfig("akka-cc"))

    val broadcaster = system_cc.actorOf(Props[BroadcastActor], name = "broadcast")
    //Logger.info(s"[AkkaCluster] join cluster")
    //join_cluster(system, is_seed)

    CounterSharding.launch_sharding(system_cc)

    system_cc
  }

  private def loadSeedNodes(nb_of_try:Int = 0):List[String] = {
   val seeds = clevercloudApi.allSeedInstanceIp() map (ip => s"akka.tcp://akka-cc@${ip._1}:${ip._2}")
    if(nb_of_try > 60){
      Logger.info(s"[AkkaCluster] too long to load seed node, we abord")
      List()
    } else if(seeds.isEmpty){
      Logger.info(s"[AkkaCluster] no seed node detected. Entering sleep for 10s ...")
      Thread.sleep(10000) // 2s

      loadSeedNodes(nb_of_try + 1 )
    }else{
      Logger.info(s"[AkkaCluster] seed nodes loaded : \n${seeds.mkString("\n")}")
      seeds
    }
  }

  private def create_system(): ActorSystem = {
    Logger.info(s"[AkkaCluster] creating system")

    val seeds = loadSeedNodes()
    Logger.info(s"[AkkaCluster] seed nodes : \n${seeds.mkString("\n")}")

    val seeds_config: java.lang.Iterable[String] = seeds.toIterable.asJava
    val currentIp = clevercloudApi.getCurrentInstanceIp()

    val is_local = configuration.getBoolean("is_local_mode").getOrElse(true)
    Logger.info(s"[AkkaCluster] currentIp = $currentIp - is local $is_local")

    val overrideConfig = if (is_local) {
      ConfigFactory.empty()
      // *************************
      // test config
      //        .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef("aac280b2.ngrok.io"))
      //        .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(currentIp._2))
      //        //.withValue("akka.remote.netty.tcp.bind-hostname", ConfigValueFactory.fromAnyRef(currentIp._1))
      //        .withValue("akka.remote.netty.tcp.bind-port", ConfigValueFactory.fromAnyRef(currentIp._2))
      //        .withValue("akka.persistence.journal.leveldb.dir", ConfigValueFactory.fromAnyRef("target/journal2"))

    } else {

      ConfigFactory.empty()
        // *************************
        // prod config
        .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(currentIp._1))
        .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(currentIp._2))
        .withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seeds_config))
    }
    //      ConfigFactory.empty()
    //        .withValue("akka.actor.provider", ConfigValueFactory.fromAnyRef("cluster"))
    //        .withValue("akka.remote.log-remote-lifecycle-events", ConfigValueFactory.fromAnyRef("off"))
    //        .withValue("akka.cluster.metrics.enabled", ConfigValueFactory.fromAnyRef("off"))
    //        .withValue("akka.extensions", ConfigValueFactory.fromAnyRef("akka.cluster.metrics.ClusterMetricsExtension"))
    //
    //        .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(currentIp._1))
    //        .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(currentIp._2))
    //        .withValue("akka.remote.netty.tcp.bind-hostname", ConfigValueFactory.fromAnyRef("127.0.0.1"))
    //        .withValue("akka.remote.netty.tcp.bind-port", ConfigValueFactory.fromAnyRef("2551"))

    //.withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seeds_config))

    //        .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef("test-cluster.particeep.com"))
    //        .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(80))
    //        .withValue("akka.remote.netty.tcp.bind-hostname", ConfigValueFactory.fromAnyRef(NetworkUtils.getIp()))
    //        .withValue("akka.remote.netty.tcp.bind-port", ConfigValueFactory.fromAnyRef(cluster_port))

    ActorSystem("akka-cc", overrideConfig withFallback ConfigFactory.load().getConfig("akka-cc"))
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

  private def dispatch_msg(broadcaster: ActorRef) = {
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
