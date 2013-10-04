package actors

case class UserPosition(userId: String, timestamp: Long, position: Position)
case class Position(lat: Double, lon: Double)
case class Area(a: Position, b: Position)