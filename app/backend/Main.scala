package backend

import akka.actor.ActorSystem
import akka.actor.Props
import actors.RegionManagerClient
import java.net.URL

/**
 * Main class for starting a backend node
 */
object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("application")
    system.actorOf(RegionManager.props(), "regionManager")

    if (Settings(system).BotsEnabled) {
      val regionManagerClient = system.actorOf(RegionManagerClient.props(), "regionManagerClient")

      def findUrls(id: Int): List[URL] = {
        val url = Option(this.getClass.getClassLoader.getResource("bots/" + id + ".json"))
        url.map(url => url :: findUrls(id + 1)).getOrElse(Nil)
      }

      new BotManager(system, regionManagerClient, findUrls(1)).start()
    }
  }
}