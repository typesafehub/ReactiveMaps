package actors

import akka.actor.{ActorRef, Actor}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._

object MockGpsBot {
  case object Step
  case class Position(lat: Double, lon: Double)
  case object Attach
  case object Detach

}

class MockGpsBot extends Actor {

  import MockGpsBot._

  val Sydney = (-33.8600, 151.2111)
  val Canberra = (-35.3075, 149.1244)
  val Points = 100

  var pos = 0
  var direction = -1
  var listeners: Set[ActorRef] = Set()

  def getCoordsForPosition(p: Int): (Double, Double) = {
    (Sydney._1 + ((Sydney._1 - Canberra._1) / Points * p), Sydney._2 + ((Sydney._2 - Canberra._2) / Points * p))
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
      listeners.foreach { a =>
        a ! Position.tupled(coords)
      }
    case Attach =>
      listeners = listeners + sender
    case Detach =>
      listeners = listeners - sender
  }
}
