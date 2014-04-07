package actors

import play.api._
import play.api.libs.concurrent.Akka
import backend._
import akka.cluster.Cluster
import java.net.URL

/**
 * Lookup for actors used by the web front end.
 */
object Actors {

  private def actors(implicit app: Application) = app.plugin[Actors]
    .getOrElse(sys.error("Actors plugin not registered"))

  /**
   * Get the region manager client.
   */
  def regionManagerClient(implicit app: Application) = actors.regionManagerClient
}

/**
 * Manages the creation of actors in the web front end.
 *
 * This is discovered by Play in the `play.plugins` file.
 */
class Actors(app: Application) extends Plugin {

  private def system = Akka.system(app)

  override def onStart() = {
    if (Cluster(system).selfRoles.exists(r => r.startsWith("backend"))) {
      system.actorOf(RegionManager.props(), "regionManager")
    }

    if (Settings(system).BotsEnabled) {
      def findUrls(id: Int): List[URL] = {
        val url = app.resource("bots/" + id + ".json")
        url.map(url => url :: findUrls(id + 1)).getOrElse(Nil)
      }
      system.actorOf(BotManager.props(regionManagerClient, findUrls(1)))
    }
  }

  private lazy val regionManagerClient = system.actorOf(RegionManagerClient.props(), "regionManagerClient")
}
