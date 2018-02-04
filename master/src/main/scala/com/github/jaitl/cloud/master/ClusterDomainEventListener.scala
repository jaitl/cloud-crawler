package com.github.jaitl.cloud.master

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberExited
import akka.cluster.ClusterEvent.MemberRemoved
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.ReachableMember
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.MemberStatus

class ClusterDomainEventListener extends Actor with ActorLogging {
  Cluster(context.system).subscribe(self, classOf[ClusterDomainEvent])

  def receive: Receive = {
    case MemberUp(member) =>
      log.info(s"$member UP.")
    case MemberExited(member) =>
      log.info(s"$member EXITED.")
    case MemberRemoved(member, previousState) =>
      if (previousState == MemberStatus.Exiting) {
        log.info(s"Member $member Previously gracefully exited, REMOVED.")
      } else {
        log.info(s"$member Previously downed after unreachable, REMOVED.")
      }
    case UnreachableMember(member) =>
      log.info(s"$member UNREACHABLE")
    case ReachableMember(member) =>
      log.info(s"$member REACHABLE")
    case state: CurrentClusterState =>
      log.info(s"Current state of the cluster: $state")
  }

  override def postStop(): Unit = {
    Cluster(context.system).unsubscribe(self)
    super.postStop()
  }
}
