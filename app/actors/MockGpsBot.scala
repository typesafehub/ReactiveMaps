package actors

import akka.actor.{ ActorRef, Actor }
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Publish

object MockGpsBot {
  case object Step
}

class MockGpsBot extends Actor {

  import MockGpsBot._

  val mediator = DistributedPubSubExtension(context.system).mediator

  val Sydney = (-33.8600, 151.2111)
  val Canberra = (-35.3075, 149.1244)
  val Points = 100

  var pos = 0
  var direction = -1

  def getCoordsForPosition(p: Int): (Double, Double) = {
    (Sydney._1 + ((Canberra._1 - Sydney._1) / Points * p), Sydney._2 + ((Canberra._2 - Sydney._2) / Points * p))
  }

  override def preStart() {
    val self = this.self
    context.system.scheduler.schedule(1 second, 1 second, self, Step)
  }

  def receive = {
    case Step =>
      if (pos == Points || pos == 0) {
        direction = -direction
      }
      pos += direction
      val coords = getCoordsForPosition(pos)
      val regionId = "dummyRegion1"
      val userPos = UserPosition("dummyUserId", System.currentTimeMillis, Position.tupled(coords))
      mediator ! Publish(regionId, userPos)
  }
}
