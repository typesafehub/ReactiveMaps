package backend

import akka.actor.ActorSystem
import actors.RegionManagerClient
import java.net.URL
import akka.cluster.Cluster

/**
 * Main class for starting a backend node.
 * A backend node can have two roles: "backend-region" and/or "backend-summary".
 * The lowest level regions run on nodes with role "backend-region".
 * Summary level regions run on nodes with role "backend-summary".
 *
 * The roles can be specfied on the sbt command line as:
 * {{{
 * sbt -Dakka.remote.netty.tcp.port=0 -Dakka.cluster.roles.1=backend-region -Dakka.cluster.roles.2=backend-summary "run-main backend.Main"
 * }}}
 *
 * If the node has role "frontend" it starts the simulation bots.
 */
object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("application")

    if (Cluster(system).selfRoles.exists(r => r.startsWith("backend"))) {
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