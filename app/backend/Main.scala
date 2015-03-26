package backend

import java.net.URL

import actors.RegionManagerClient
import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.typesafe.conductr.bundlelib
import com.typesafe.conductr.bundlelib.Env
import com.typesafe.conductr.bundlelib.akka.{ClusterProperties, StatusService}

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
    implicit val system = ActorSystem("application")

    // if running with ConductR we utilise it's cluster discovery capabilities
    if (Env.isRunByConductR)
      ClusterProperties.initialize() // prepares seed node env variables based on information from ConductR

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

    // if using it, signal ConductR that we initialized successfully
    if (Env.isRunByConductR)
      StatusService.signalStartedOrExit()(bundlelib.akka.ConnectionContext())
  }

}