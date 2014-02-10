package actors

import akka.actor._

object NameService {
    case class Put(email: String, name: String)
    case class Get(email: String)
    case class GetResponse(email: String, name: Option[String])
    
    def props(): Props = Props(new NameService)
}

class NameService extends Actor {
    import NameService._
    
    var theMap = Map.empty[String, String]
    
    def receive = {
        case Put(email, name) => theMap += email -> name
        case Get(email) => sender ! GetResponse(email, theMap.get(email))
    }
}