package backend

import akka.actor.ActorSystem
import akka.actor.Props

object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("application")
    system.actorOf(Props[RegionManager], "regionManager")
  }
}