package actor

import akka.actor._
import akka.cluster.sharding.{ClusterShardingSettings, ClusterSharding, ShardRegion}
import akka.persistence.PersistentActor

import scala.concurrent.duration._

case object Increment
case object Decrement
final case class Get(counterId: Long)
final case class EntityEnvelope(id: Long, payload: Any)

case object Stop
final case class CounterChanged(delta: Int)

class Counter extends PersistentActor with ActorLogging {
  import ShardRegion.Passivate

  context.setReceiveTimeout(120.seconds)

  // self.path.name is the entity identifier (utf-8 URL-encoded)
  override def persistenceId: String = "Counter-" + self.path.name

  var count = 0

  def updateState(event: CounterChanged): Unit =
    count += event.delta

  override def receiveRecover: Receive = {
    case evt: CounterChanged => updateState(evt)
  }

  override def receiveCommand: Receive = {
    case Increment => {
      log.warning("Increment")
      persist(CounterChanged(+1))(updateState)
    }
    case Decrement => {
      log.warning("Decrement")
      persist(CounterChanged(-1))(updateState)
    }
    case Get(x) => {
      log.warning(s"GET $x")
      sender() ! count
    }
    case ReceiveTimeout => {
      log.warning("ReceiveTimeout")
      context.parent ! Passivate(stopMessage = Stop)
    }
    case Stop => context.stop(self)
  }
}

object CounterSharding {
  def launch_sharding(system: ActorSystem) = {
    val counterRegion: ActorRef = ClusterSharding(system).start(
      typeName = "Counter",
      entityProps = Props[Counter],
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId
    )

    counterRegion
  }

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case EntityEnvelope(id, payload) => (id.toString, payload)
    case msg @ Get(id)               => (id.toString, msg)
  }

  val numberOfShards = 2

  val extractShardId: ShardRegion.ExtractShardId = {
    case EntityEnvelope(id, _) => (id % numberOfShards).toString
    case Get(id)               => (id % numberOfShards).toString
  }

  def test(system: ActorSystem) = {
    val counterRegion: ActorRef = ClusterSharding(system).shardRegion("Counter")
    counterRegion ! Get(123)
    //expectMsg(0)

    counterRegion ! EntityEnvelope(123, Increment)
    counterRegion ! Get(123)
    //expectMsg(1)
  }
}
