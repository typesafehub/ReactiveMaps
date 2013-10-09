package models.geojson

import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.runtime.AbstractPartialFunction

/**
 * These are the raw "internal" formats.  They do not add the crs parameter.
 */
private object GeoFormats {
  implicit def crsBoxFormat[C](implicit cFormat: Format[C]): Format[(C, C)] = Format(
    Reads[(C, C)] {
      case JsArray(seq) =>
        val (first, second) = seq.splitAt(seq.size / 2)
        for {
          f <- cFormat.reads(JsArray(first))
          s <- cFormat.reads(JsArray(second))
        } yield (f, s)
      case _ => JsError("bbox must be an array")
    }, Writes { (bbox: (C, C)) =>
      (cFormat.writes(bbox._1), cFormat.writes(bbox._2)) match {
        case (a: JsArray, b: JsArray) => a ++ b
        case _ => throw new RuntimeException("CRS format writes must produce a JsArray")
      }
    }
  )

  implicit def pointFormat[C : Format]: Format[Point[C]] = (
    (__ \ "type").format[String] ~
    (__ \ "coordinates").format[C] ~
    (__ \ "bbox").formatNullable[(C, C)]
  ).apply({
    case ("Point", coords, bbox) => Point(coords, bbox)
  }, point => ("Point", point.coordinates, point.bbox))

  implicit def multiPointFormat[C : Format]: Format[MultiPoint[C]] = (
    (__ \ "type").format[String] ~
    (__ \ "coordinates").format[Seq[C]] ~
    (__ \ "bbox").formatNullable[(C, C)]
  ).apply({
    case ("MultiPoint", coords, bbox) => MultiPoint(coords, bbox)
  }, multiPoint => ("MultiPoint", multiPoint.coordinates, multiPoint.bbox))

  implicit def lineStringFormat[C : Format]: Format[LineString[C]] = (
    (__ \ "type").format[String] ~
    (__ \ "coordinates").format[Seq[C]] ~
    (__ \ "bbox").formatNullable[(C, C)]
  ).apply({
    case ("LineString", coords, bbox) => LineString(coords, bbox)
  }, lineString => ("LineString", lineString.coordinates, lineString.bbox))

  implicit def multiLineStringFormat[C : Format]: Format[MultiLineString[C]] = (
    (__ \ "type").format[String] ~
    (__ \ "coordinates").format[Seq[Seq[C]]] ~
    (__ \ "bbox").formatNullable[(C, C)]
  ).apply({
    case ("MultiLineString", coords, bbox) => MultiLineString(coords, bbox)
  }, multiLineString => ("MultiLineString", multiLineString.coordinates, multiLineString.bbox))

  implicit def polygonFormat[C : Format]: Format[Polygon[C]] = (
    (__ \ "type").format[String] ~
    (__ \ "coordinates").format[Seq[C]] ~
    (__ \ "bbox").formatNullable[(C, C)]
  ).apply({
    case ("Polygon", coords, bbox) => Polygon(coords, bbox)
  }, polygon => ("Polygon", polygon.coordinates, polygon.bbox))

  implicit def multiPolygonFormat[C : Format]: Format[MultiPolygon[C]] = (
    (__ \ "type").format[String] ~
    (__ \ "coordinates").format[Seq[Seq[C]]] ~
    (__ \ "bbox").formatNullable[(C, C)]
  ).apply({
    case ("MultiPolygon", coords, bbox) => MultiPolygon(coords, bbox)
  }, multiPolygon => ("MultiPolygon", multiPolygon.coordinates, multiPolygon.bbox))

  implicit def geometryCollectionFormat[C : Format]: Format[GeometryCollection[C]] = (
    (__ \ "type").format[String] ~
    (__ \ "geometries").format[Seq[Geometry[C]]] ~
    (__ \ "bbox").formatNullable[(C, C)]
  ).apply({
    case ("GeometryCollection", geometries, bbox) => GeometryCollection(geometries, bbox)
  }, geometryCollection => ("GeometryCollection", geometryCollection.geometries, geometryCollection.bbox))

  implicit def featureFormat[C : Format]: Format[Feature[C]] = (
    (__ \ "type").format[String] ~
    (__ \ "geometry").format[Geometry[C]] ~
    (__ \ "properties").formatNullable[JsObject] ~
    (__ \ "id").formatNullable[String] ~
    (__ \ "bbox").formatNullable[(C, C)]
  ).apply({
    case ("Feature", geometry, properties, id, bbox) => Feature(geometry, properties, id, bbox)
  }, feature => ("Feature", feature.geometry, feature.properties, feature.id, feature.bbox))

  implicit def featureCollectionFormat[C : Format]: Format[FeatureCollection[C]] = (
    (__ \ "type").format[String] ~
    (__ \ "features").format[Seq[Feature[C]]] ~
    (__ \ "bbox").formatNullable[(C, C)]
  ).apply({
    case ("FeatureCollection", features, bbox) => FeatureCollection(features, bbox)
  }, featureCollection => ("FeatureCollection", featureCollection.features, featureCollection.bbox))

  implicit def geometryFormat[C : Format]: Format[Geometry[C]] = Format(
    Reads { json =>
      (json \ "type").asOpt[String] match {
        case Some("Point") => json.validate(pointFormat[C])
        case Some("MultiPoint") => json.validate(multiPointFormat[C])
        case Some("LineString") => json.validate(lineStringFormat[C])
        case Some("MultiLineString") => json.validate(multiLineStringFormat[C])
        case Some("Polygon") => json.validate(polygonFormat[C])
        case Some("MultiPolygon") => json.validate(multiPolygonFormat[C])
        case Some("GeometryCollection") => json.validate(geometryCollectionFormat[C])
        case _ => JsError("Not a geometry")
      }
    },
    Writes {
      case point: Point[C] => pointFormat[C].writes(point)
      case multiPoint: MultiPoint[C] => multiPointFormat[C].writes(multiPoint)
      case lineString: LineString[C] => lineStringFormat[C].writes(lineString)
      case multiLineString: MultiLineString[C] => multiLineStringFormat[C].writes(multiLineString)
      case polygon: Polygon[C] => polygonFormat[C].writes(polygon)
      case multiPolygon: MultiPolygon[C] => multiPolygonFormat[C].writes(multiPolygon)
      case geometryCollection: GeometryCollection[C] => geometryCollectionFormat[C].writes(geometryCollection)
    }
  )

  def geoJsonFormat[C: Format]: Format[GeoJson[C]] = Format(
    Reads { json =>
      (json \ "type").asOpt[String] match {
        case Some("Point") => json.validate(pointFormat[C])
        case Some("MultiPoint") => json.validate(multiPointFormat[C])
        case Some("LineString") => json.validate(lineStringFormat[C])
        case Some("MultiLineString") => json.validate(multiLineStringFormat[C])
        case Some("Polygon") => json.validate(polygonFormat[C])
        case Some("MultiPolygon") => json.validate(multiPolygonFormat[C])
        case Some("GeometryCollection") => json.validate(geometryCollectionFormat[C])
        case Some("Feature") => json.validate(featureFormat[C])
        case Some("FeatureCollection") => json.validate(featureCollectionFormat[C])
        case _ => JsError("Not a geometry")
      }
    },
    Writes {
      case point: Point[C] => pointFormat[C].writes(point)
      case multiPoint: MultiPoint[C] => multiPointFormat[C].writes(multiPoint)
      case lineString: LineString[C] => lineStringFormat[C].writes(lineString)
      case multiLineString: MultiLineString[C] => multiLineStringFormat[C].writes(multiLineString)
      case polygon: Polygon[C] => polygonFormat[C].writes(polygon)
      case multiPolygon: MultiPolygon[C] => multiPolygonFormat[C].writes(multiPolygon)
      case geometryCollection: GeometryCollection[C] => geometryCollectionFormat[C].writes(geometryCollection)
      case feature: Feature[C] => featureFormat[C].writes(feature)
      case featureCollection: FeatureCollection[C] => featureCollectionFormat[C].writes(featureCollection)
    }
  )

  def writesWithCrs[C, G](writes: Writes[G])(implicit crs: CrsFormat[C]) = writes.transform { json =>
    if (crs.isDefault) {
      json
    } else {
      json match {
        case obj: JsObject => obj ++ Json.obj("crs" -> crs.crs)
        case other => other
      }
    }
  }

}

case class LatLong(x: Double, y: Double)

sealed trait GeoJson[C] {
  val bbox: Option[(C, C)]
}

object GeoJson {
  implicit def geoJsonWrites[C](implicit crs: CrsFormat[C]): Writes[GeoJson[C]] = GeoFormats.writesWithCrs(GeoFormats.geoJsonFormat[C](crs.format))
  implicit def geoJsonReads[C](implicit crs: CrsFormat[C]): Reads[GeoJson[C]] = GeoFormats.geoJsonFormat(crs.format)
}

/*
case class GeoJsonWithCrs[G <: GeoJson[_]](geoJson: G, crs: Option[Crs]) {
  def as[C](implicit format: CrsFormat[C]): Option[GeoJson[C]] = {
    (crs, format.crs) match {
      case (None, Wrs84Format.crs) => Some(this.asInstanceOf[GeoJson[C]])
      case (e1, e2) if e1 == e2 => Some(this.asInstanceOf[GeoJson[C]])
      case _ => None
    }
  }
}

object GeoJsonWithCrs {
  def reads[G <: GeoJson[_]](reads: Format[_] => Reads[G])(implicit crsMapper: CrsMapper): Reads[GeoJsonWithCrs[G]] =
    (__ \ "crs").readNullable[Crs].flatMap { maybeCrs =>
      maybeCrs.map { crs =>
        crsMapper.lift(crs).map { crsFormat =>
          reads(crsFormat.format)
        }.getOrElse {
          Reads(_ => JsError("Unknown crs: " + crs))
        }
      }.getOrElse(reads(Wrs84Format.format)).map { geoJson =>
        GeoJsonWithCrs(geoJson, maybeCrs)
      }
    }
}
*/

sealed trait Geometry[C] extends GeoJson[C]

object Geometry {
  implicit def geometryReads[C](implicit crs: CrsFormat[C]): Reads[Geometry[C]] = GeoFormats.geometryFormat(crs.format)
}

case class Point[C](coordinates: C, bbox: Option[(C, C)] = None) extends Geometry[C]

object Point {
  implicit def pointReads[C](implicit crs: CrsFormat[C]): Reads[Point[C]] = GeoFormats.pointFormat(crs.format)
}

case class MultiPoint[C](coordinates: Seq[C], bbox: Option[(C, C)] = None) extends Geometry[C]

object MultiPoint {
  implicit def multiPointReads[C](implicit crs: CrsFormat[C]): Reads[MultiPoint[C]] = GeoFormats.multiPointFormat(crs.format)
}

case class LineString[C](coordinates: Seq[C], bbox: Option[(C, C)] = None) extends Geometry[C]

object LineString {
  implicit def lineStringReads[C](implicit crs: CrsFormat[C]): Reads[LineString[C]] = GeoFormats.lineStringFormat(crs.format)
}

case class MultiLineString[C](coordinates: Seq[Seq[C]], bbox: Option[(C, C)] = None) extends Geometry[C]

object MultiLineString {
  implicit def multiLineStringReads[C](implicit crs: CrsFormat[C]): Reads[MultiLineString[C]] = GeoFormats.multiLineStringFormat(crs.format)
}

case class Polygon[C](coordinates: Seq[C], bbox: Option[(C, C)] = None) extends Geometry[C]

object Polygon {
  implicit def polygonReads[C](implicit crs: CrsFormat[C]): Reads[Polygon[C]] = GeoFormats.polygonFormat(crs.format)
}

case class MultiPolygon[C](coordinates: Seq[Seq[C]], bbox: Option[(C, C)] = None) extends Geometry[C]

object MultiPolygon {
  implicit def multiPolygonReads[C](implicit crs: CrsFormat[C]): Reads[MultiPolygon[C]] = GeoFormats.multiPolygonFormat(crs.format)
}

case class GeometryCollection[C](geometries: Seq[Geometry[C]], bbox: Option[(C, C)] = None) extends Geometry[C]

object GeometryCollection {
  implicit def geometryCollectionReads[C](implicit crs: CrsFormat[C]): Reads[GeometryCollection[C]] = GeoFormats.geometryCollectionFormat(crs.format)
}

case class FeatureCollection[C](features: Seq[Feature[C]], bbox: Option[(C, C)] = None) extends GeoJson[C]

object FeatureCollection {
  implicit def featureCollectionReads[C](implicit crs: CrsFormat[C]): Reads[FeatureCollection[C]] = GeoFormats.featureCollectionFormat(crs.format)
}

case class Feature[C](geometry: Geometry[C], properties: Option[JsObject] = None, id: Option[String] = None, bbox: Option[(C, C)] = None) extends GeoJson[C]

object Feature {
  implicit def featureReads[C](implicit crs: CrsFormat[C]): Reads[Feature[C]] = GeoFormats.featureFormat(crs.format)
}

sealed trait Crs

object Crs {
  implicit def crsFormat: Format[Crs] = Format(
    Reads { json =>
      NamedCrs.namedCrsFormat.reads(json).or(LinkedCrs.linkedCrsFormat.reads(json))
    },
    Writes {
      case named: NamedCrs => NamedCrs.namedCrsFormat.writes(named)
      case linked: LinkedCrs => LinkedCrs.linkedCrsFormat.writes(linked)
    }
  )
}

case class NamedCrs(name: String) extends Crs

object NamedCrs {
  implicit def namedCrsFormat = Json.format[NamedCrs]
}

case class LinkedCrs(href: String, `type`: Option[String]) extends Crs

object LinkedCrs {
  implicit def linkedCrsFormat = Json.format[LinkedCrs]
}

trait CrsMapper extends PartialFunction[Crs, CrsFormat[_]]

object CrsMapper {
  def apply(mapper: PartialFunction[Crs, CrsFormat[_]]) = new CrsMapper {
    def apply(v1: Crs) = mapper.apply(v1)
    def isDefinedAt(x: Crs) = mapper.isDefinedAt(x)
    override def applyOrElse[A1 <: Crs, B1 >: CrsFormat[_]](x: A1, default: (A1) => B1) = mapper.applyOrElse(x, default)
  }
}

object CrsFormat {
  def fromJson(json: JsValue)(implicit crsMapper: CrsMapper): CrsFormat[_] = {
    (json \ "crs").asOpt[Crs].map { crs =>
      crsMapper.lift(crs).getOrElse(throw new RuntimeException("No CRS format found for CRS: " + crs))
    }.getOrElse(Wgs84Format)
  }
}

trait CrsFormat[C] {
  def crs: Crs
  def format: Format[C]
  def isDefault = false

  def as[D](implicit that: CrsFormat[D]): CrsFormat[D] = {
    that match {
      case same if this == that => that
      case _ => throw new RuntimeException(that + " is not " + this)
    }
  }
}

object Wgs84Format extends CrsFormat[LatLong] {
  val crs = NamedCrs("urn:ogc:def:crs:OGC:1.3:CRS84")
  val format = Format[LatLong](
    __.read[Seq[Double]].map {
      case Seq(x, y) => LatLong(x, y)
    }, Writes(latLong => Json.arr(latLong.x, latLong.y))
  )

  override def isDefault = true
}

