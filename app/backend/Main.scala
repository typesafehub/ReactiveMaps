package backend

import akka.actor.ActorSystem
import actors.RegionManagerClient
import java.net.URL
import akka.cluster.Cluster

/**
 * Main class for starting a backend node.
 * sbt -Dakka.remote.netty.tcp.port=0 "run-main backend.Main"
 */
object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("application")

    if (Cluster(system).selfRoles.contains("backend")) {
      system.actorOf(RegionManager.props(), "regionManager")
    }

    if (Settings(system).BotsEnabled && Cluster(system).selfRoles.contains("frontend")) {
      val regionManagerClient = system.actorOf(RegionManagerClient.props(), "regionManagerClient")

      def findUrls(id: Int): List[URL] = {
        val url = Option(this.getClass.getClassLoader.getResource("bots/" + id + ".json"))
        url.map(url => url :: findUrls(id + 1)).getOrElse(Nil)
      }

      system.actorOf(BotManager.props(regionManagerClient, findUrls(1)))
    }
  }
}