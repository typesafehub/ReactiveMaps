package actors

import akka.actor.{ ActorRef, Actor }
import scala.concurrent.duration._
import backend.UserPosition
import java.util.concurrent.atomic.AtomicInteger
import java.util.UUID
import models.geojson.LatLng

object MockGpsBot {
  case object Step
}

class MockGpsBot(regionManagerClient: ActorRef) extends Actor {

  import MockGpsBot._

  val Sydney = (-33.8600, 151.2111)
  val Canberra = (-35.3075, 149.1244)
  val Points = 100

  val userId = "bot-" + UUID.randomUUID()

  var pos = 0
  var direction = -1

  def getCoordsForPosition(p: Int): (Double, Double) = {
    (Sydney._1 + ((Canberra._1 - Sydney._1) / Points * p), Sydney._2 + ((Canberra._2 - Sydney._2) / Points * p))
  }

  import context.dispatcher
  val stepTask = context.system.scheduler.schedule(1 second, 1 second, context.self, Step)

  override def postStop(): Unit = stepTask.cancel()

  def receive = {
    case Step =>
      if (pos == Points || pos == 0) {
        direction = -direction
      }
      pos += direction
      val coords = getCoordsForPosition(pos)
      val userPos = UserPosition(userId, System.currentTimeMillis, LatLng(coords._1, coords._2))
      regionManagerClient ! userPos
  }
}
