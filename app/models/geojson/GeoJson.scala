package models.geojson

import scala.collection.immutable.Seq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * A GeoJSON object.
 *
 * @tparam C The object used to model the CRS that this GeoJSON object uses.
 */
sealed trait GeoJson[C] {
  val bbox: Option[(C, C)]
}

object GeoJson {
  implicit def geoJsonWrites[C](implicit crs: CrsFormat[C]): Writes[GeoJson[C]] = GeoFormats.writesWithCrs(GeoFormats.geoJsonFormat[C](crs.format))
  implicit def geoJsonReads[C](implicit crs: CrsFormat[C]): Reads[GeoJson[C]] = GeoFormats.geoJsonFormat(crs.format)
}

/**
 * A GeoJSON Geometry object.
 *
 * @tparam C The object used to model the CRS that this GeoJSON object uses.
 */
sealed trait Geometry[C] extends GeoJson[C]

object Geometry {
  implicit def geometryReads[C](implicit crs: CrsFormat[C]): Reads[Geometry[C]] = GeoFormats.geometryFormat(crs.format)
}

/**
 * A GeoJSON Point object.
 *
 * @param coordinates The coordinates of this point.
 * @param bbox The bounding box of the point, if any.
 * @tparam C The object used to model the CRS that this GeoJSON object uses.
 */
case class Point[C](coordinates: C, bbox: Option[(C, C)] = None) extends Geometry[C]

object Point {
  implicit def pointReads[C](implicit crs: CrsFormat[C]): Reads[Point[C]] = GeoFormats.pointFormat(crs.format)
}

/**
 * A GeoJSON MultiPoint object.
 *
 * @param coordinates The sequence coordinates for the points.
 * @param bbox The bounding box for the points, if any.
 * @tparam C The object used to model the CRS that this GeoJSON object uses.
 */
case class MultiPoint[C](coordinates: Seq[C], bbox: Option[(C, C)] = None) extends Geometry[C]

object MultiPoint {
  implicit def multiPointReads[C](implicit crs: CrsFormat[C]): Reads[MultiPoint[C]] = GeoFormats.multiPointFormat(crs.format)
}

/**
 * A GeoJSON LineString object.
 *
 * @param coordinates The sequence of coordinates for the line.
 * @param bbox The bounding box for the line, if any.
 * @tparam C The object used to model the CRS that this GeoJSON object uses.
 */
case class LineString[C](coordinates: Seq[C], bbox: Option[(C, C)] = None) extends Geometry[C]

object LineString {
  implicit def lineStringReads[C](implicit crs: CrsFormat[C]): Reads[LineString[C]] = GeoFormats.lineStringFormat(crs.format)
}

/**
 * A GeoJSON MultiLineString object.
 *
 * @param coordinates The sequence of lines.
 * @param bbox The bounding box for the lines, if any.
 * @tparam C The object used to model the CRS that this GeoJSON object uses.
 */
case class MultiLineString[C](coordinates: Seq[Seq[C]], bbox: Option[(C, C)] = None) extends Geometry[C]

object MultiLineString {
  implicit def multiLineStringReads[C](implicit crs: CrsFormat[C]): Reads[MultiLineString[C]] = GeoFormats.multiLineStringFormat(crs.format)
}

/**
 * A GeoJSON Polygon object.
 *
 * @param coordinates The sequence of corners in the polygon.
 * @param bbox The bounding box for the polygon, if any.
 * @tparam C The object used to model the CRS that this GeoJSON object uses.
 */
case class Polygon[C](coordinates: Seq[C], bbox: Option[(C, C)] = None) extends Geometry[C]

object Polygon {
  implicit def polygonReads[C](implicit crs: CrsFormat[C]): Reads[Polygon[C]] = GeoFormats.polygonFormat(crs.format)
}

/**
 * A GeoJSON MultiPolygon object.
 *
 * @param coordinates The sequence of polygons.
 * @param bbox The bounding box for the polygons, if any.
 * @tparam C The object used to model the CRS that this GeoJSON object uses.
 */
case class MultiPolygon[C](coordinates: Seq[Seq[C]], bbox: Option[(C, C)] = None) extends Geometry[C]

object MultiPolygon {
  implicit def multiPolygonReads[C](implicit crs: CrsFormat[C]): Reads[MultiPolygon[C]] = GeoFormats.multiPolygonFormat(crs.format)
}

/**
 * A GeoJSON GeometryCollection object.
 *
 * @param geometries The sequence of geometries.
 * @param bbox The bounding box for the geometries, if any.
 * @tparam C The object used to model the CRS that this GeoJSON object uses.
 */
case class GeometryCollection[C](geometries: Seq[Geometry[C]], bbox: Option[(C, C)] = None) extends Geometry[C]

object GeometryCollection {
  implicit def geometryCollectionReads[C](implicit crs: CrsFormat[C]): Reads[GeometryCollection[C]] = GeoFormats.geometryCollectionFormat(crs.format)
}

/**
 * A GeoJSON FeatureCollection object.
 *
 * @param features The sequence of features.
 * @param bbox The bounding box for the sequence of features, if any.
 * @tparam C The object used to model the CRS that this GeoJSON object uses.
 */
case class FeatureCollection[C](features: Seq[Feature[C]], bbox: Option[(C, C)] = None) extends GeoJson[C]

object FeatureCollection {
  implicit def featureCollectionReads[C](implicit crs: CrsFormat[C]): Reads[FeatureCollection[C]] = GeoFormats.featureCollectionFormat(crs.format)
}

/**
 * A GeoJSON Feature object.
 *
 * @param geometry The geometry for the feature.
 * @param properties The properties for the feature, if any.
 * @param id The id of the feature, if any.
 * @param bbox The bounding box for the feature, if any.
 * @tparam C The object used to model the CRS that this GeoJSON object uses.
 */
case class Feature[C](geometry: Geometry[C], properties: Option[JsObject] = None, id: Option[JsValue] = None, bbox: Option[(C, C)] = None) extends GeoJson[C]

object Feature {
  implicit def featureReads[C](implicit crs: CrsFormat[C]): Reads[Feature[C]] = GeoFormats.featureFormat(crs.format)
}

/**
 * A GeoJSON coordinate reference system (CRS).
 */
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

/**
 * A GeoJSON named CRS.
 *
 * @param name The name of the CRS.
 */
case class NamedCrs(name: String) extends Crs

object NamedCrs {
  implicit def namedCrsFormat = Json.format[NamedCrs]
}

/**
 * A GeoJSON linked CRS.
 *
 * @param href The href for the CRS.
 * @param type The type of the link, if any.
 */
case class LinkedCrs(href: String, `type`: Option[String]) extends Crs

object LinkedCrs {
  implicit def linkedCrsFormat = Json.format[LinkedCrs]
}

/**
 * A latitude longitude CRS, for use with WGS84.
 *
 * @param lat The latitude.
 * @param lng The longitude.
 */
case class LatLng(lat: Double, lng: Double)

object LatLng {
  implicit def latLngFormat: Format[LatLng] = Wgs84Format.format
  implicit def latLngCrs: CrsFormat[LatLng] = Wgs84Format
}

/**
 * A CRS format
 */
trait CrsFormat[C] {
  /**
   * The CRS for the CRS format
   */
  def crs: Crs

  /**
   * The format to use to write the CRS.
   */
  def format: Format[C]

  /**
   * Whether this is the default CRS format.  If so, no CRS information will be added to the GeoJSON object when
   * serialised.
   */
  def isDefault = false
}

/**
 * The WGS84 CRS format.
 */
object Wgs84Format extends CrsFormat[LatLng] {
  val crs = NamedCrs("urn:ogc:def:crs:OGC:1.3:CRS84")
  val format = Format[LatLng](
    __.read[Seq[Double]].map {
      case Seq(lng, lat) => LatLng(lat, lng)
    }, Writes(latLng => Json.arr(latLng.lng, latLng.lat))
  )

  override def isDefault = true
}

/**
 * These are the raw "internal" formats.  They do not add the CRS parameter when serialising.
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
      // The spec isn't clear on what the id can be
      (__ \ "id").formatNullable[JsValue] ~
      (__ \ "bbox").formatNullable[(C, C)]
    ).apply({
    case ("Feature", geometry, properties, id, bbox) =>
      Feature(geometry, properties, id, bbox)
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
