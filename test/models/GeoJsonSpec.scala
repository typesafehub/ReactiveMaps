package models

import org.specs2.mutable.Specification
import play.api.libs.json._

object GeoJsonSpec extends Specification {

  case class MyCrs(x: Double, y: Double)

  object CustomCrs extends CrsFormat[MyCrs] {
    val crs = NamedCrs("mycrs")
    val format = Format[MyCrs](
      __.read[Seq[Double]].map {
        case Seq(x, y) => MyCrs(x, y)
      }, Writes(tt => Json.arr(tt.x, tt.y))
    )
  }

  implicit val mapper = CrsMapper {
    case Wgs84Format.crs => Wgs84Format
  }

  "GeoJson" should {
    "support points" in {
      "deserialisation" in {
        val json = Json.obj("type" -> "Point", "coordinates" -> Json.arr(1.2, 2.3))
        implicit val crsFormat = CrsFormat.fromJson(json).as(Wgs84Format)
        val point = Json.fromJson[Point[LatLong]](json).asOpt
        point must beSome.like {
          case p => p.coordinates must_== LatLong(1.2, 2.3)
        }
      }
      "serialisation" in {
        implicit val crsFormat = Wgs84Format
        val json = Json.toJson(Point(LatLong(1.2, 2.3)))
        (json \ "type").as[String] must_== "Point"
        (json \ "coordinates").as[JsArray] must_== Json.arr(1.2, 2.3)
        (json \ "crs").asOpt[JsValue] must_== None
      }
    }
    "support features" in {
      "deserialisation" in {
        val json = Json.obj("type" -> "Feature", "geometry" -> Json.obj("type" -> "Point", "coordinates" ->Json.arr(1.2, 2.3)))
        implicit val crsFormat = CrsFormat.fromJson(json).as(Wgs84Format)
        val feature = Json.fromJson[Feature[LatLong]](json).asOpt
        feature must beSome.like {
          case Feature(p: Point[LatLong], None, None, None) => p.coordinates must_== LatLong(1.2, 2.3)
        }
      }
      "serialisation" in {
        implicit val crsFormat = Wgs84Format
        val json = Json.toJson(Feature(Point(LatLong(1.2, 2.3))))
        (json \ "type").as[String] must_== "Feature"
        (json \ "geometry" \ "type").as[String] must_== "Point"
        (json \ "geometry" \ "coordinates").as[JsArray] must_== Json.arr(1.2, 2.3)
        (json \ "crs").asOpt[JsValue] must_== None
      }
    }
    // "support more tests" in { later }
  }

}
