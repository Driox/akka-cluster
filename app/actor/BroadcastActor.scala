package actor

import akka.actor.{ActorLogging, RootActorPath, Actor}
import akka.cluster.{Member, Cluster}
import akka.cluster.ClusterEvent._
import play.api.Logger

class BroadcastActor extends Actor with ActorLogging {

  import BroadcastActor._

  private val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(
      self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember]
    )
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  private var members = Set.empty[Member]
  def receive = {
    case message: String =>
      Logger.warn(s"Message from [${sender().path.toString}] : [$message]")
    case Message(content) =>
      members foreach (pathOf(_) ! content)
    case MemberUp(member) =>
      members += member
    case MemberRemoved(member, previousStatus) =>
      members.find(_.address == member.address) foreach (members -= _)
    case _: MemberEvent =>
    // ignore other events about members
  }

  def pathOf(member: Member) = {
    context.actorSelection(RootActorPath(member.address) / "user" / self.path.name)
  }
}

object BroadcastActor {
  case class Message(content: String)
}
