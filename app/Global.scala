import akka.actor.Props
import backend._
import java.net.URL
import play.api.libs.concurrent.Akka
import play.api._

object Global extends GlobalSettings {
  override def onStart(app: Application) = {

    val system = Akka.system(app)

    if (Settings(system).BotsEnabled) {
      def findUrls(id: Int): List[URL] = {
        val url = app.resource("bots/" + id + ".json")
        url.map(url => url :: findUrls(id + 1)).getOrElse(Nil)
      }
      system.actorOf(BotManager.props(controllers.Application.regionManager, findUrls(1)))
    }

  }
}
