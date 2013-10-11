package backend

import akka.actor.ActorSystem
import akka.actor.Props
import actors.RegionManagerClient
import java.net.URL

object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("application")
    system.actorOf(Props[RegionManager], "regionManager")

    if (java.lang.Boolean.getBoolean("bots.enabled")) {
      val regionManagerClient = system.actorOf(Props[RegionManagerClient], "regionManagerClient")

      def findUrls(id: Int): List[URL] = {
        val url = Option(this.getClass.getClassLoader.getResource("bots/" + id + ".json"))
        url.map(url => url :: findUrls(id + 1)).getOrElse(Nil)
      }

      new BotManager(system, regionManagerClient, findUrls(1)).start()
    }
  }
}