package actors

import akka.actor.{ActorRef, Actor}
import MockGpsBot._

class Client(bot: ActorRef, publish: Position => Unit) extends Actor {

  override def preStart() {
    bot ! Attach
  }

  override def postStop() {
    bot ! Detach
  }

  def receive = {
    case p: Position => publish(p)
  }
}
