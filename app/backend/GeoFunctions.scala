package backend

import scala.collection.immutable.IndexedSeq
import play.extras.geojson.LatLng
import models.backend._
import scala.Some
import models.backend.BoundingBox
import models.backend.Cluster
import models.backend.UserPosition
import scala.annotation.tailrec

/**
 * Geo functions.
 */
class GeoFunctions(settings: Settings) {

  /**
   * Get the region for the given point.
   *
   * @param point The point.
   * @param zoomDepth The zoom depth.
   * @return The id of the region at the given zoom depth.
   */
  def regionForPoint(point: LatLng, zoomDepth: Int = settings.MaxZoomDepth): RegionId = {
    require(zoomDepth <= settings.MaxZoomDepth, "Too deep!")
    val axisSteps = 1l << zoomDepth
    val xStep = 360d / axisSteps
    val x = Math.floor((point.lng + 180) / xStep).asInstanceOf[Int]
    val yStep = 180d / axisSteps
    val y = Math.floor((point.lat + 90) / yStep).asInstanceOf[Int]
    RegionId(zoomDepth, x, y)
  }

  /**
   * Get the regions for the given bounding box.
   *
   * @param bbox The bounding box.
   * @return The regions
   */
  def regionsForBoundingBox(bbox: BoundingBox): Set[RegionId] = {
    @tailrec def regionsAtZoomLevel(zoomLevel: Int): Set[RegionId] = {
      if (zoomLevel == 0) {
        Set(RegionId(0, 0, 0))
      } else {
        val axisSteps = 1 << zoomLevel
        // First, we get the regions that the bounds are in
        val southWestRegion = regionForPoint(bbox.southWest, zoomLevel)
        val northEastRegion = regionForPoint(bbox.northEast, zoomLevel)
        // Now calculate the width of regions we need, we need to add 1 for it to be inclusive of both end regions
        val xLength = northEastRegion.x - southWestRegion.x + 1
        val yLength = northEastRegion.y - southWestRegion.y + 1
        // Check if the number of regions is in our bounds
        val numRegions = xLength * yLength
        if (numRegions <= 0) {
          Set(RegionId(0, 0, 0))
        } else if (settings.MaxSubscriptionRegions >= numRegions) {
          // Generate the sequence of regions
          (0 until numRegions).map { i =>
            val y = i / xLength
            val x = i % xLength
            // We need to mod positive the x value, because it's possible that the bounding box started or ended from
            // less than -180 or greater than 180 W/E.
            RegionId(zoomLevel, modPositive(southWestRegion.x + x, axisSteps), southWestRegion.y + y)
          }(collection.breakOut)
        } else {
          regionsAtZoomLevel(zoomLevel - 1)
        }
      }
    }
    regionsAtZoomLevel(settings.MaxZoomDepth)
  }

  /**
   * Get the bounding box for the given region.
   */
  def boundingBoxForRegion(regionId: RegionId): BoundingBox = {
    val axisSteps = 1l << regionId.zoomLevel
    val yStep = 180d / axisSteps
    val xStep = 360d / axisSteps
    val latRegion = regionId.y * yStep - 90
    val lngRegion = regionId.x * xStep - 180

    BoundingBox(
      LatLng(latRegion, lngRegion),
      LatLng(latRegion + yStep, lngRegion + xStep))
  }

  def summaryRegionForRegion(regionId: RegionId): Option[RegionId] = {
    if (regionId.zoomLevel == 0) None
    else Some(RegionId(regionId.zoomLevel - 1, regionId.x >>> 1, regionId.y >>> 1))
  }

  /**
   * Cluster the given points into n2 boxes
   *
   * @param id The id of the region
   * @param bbox The bounding box within which to cluster
   * @param points The points to cluster
   * @return The clustered points
   */
  def cluster(id: String, bbox: BoundingBox, points: IndexedSeq[PointOfInterest]): IndexedSeq[PointOfInterest] = {
    if (points.size > settings.ClusterThreshold) {
      groupNBoxes(bbox, settings.ClusterDimension, points).map {
        case (_, IndexedSeq(single)) => single
        // The fold operation here normalises all points to making the west of the bounding box 0, and then takes an average
        case (segment, multiple) =>
          val (lng, lat, count) = multiple.foldLeft((0d, 0d, 0l)) { (totals, next) =>
            val normalisedWest = modPositive(next.position.lng + 180d, 360)
            next match {
              case u: UserPosition     => (totals._1 + normalisedWest, totals._2 + next.position.lat, totals._3 + 1)
              case Cluster(_, _, _, c) => (totals._1 + normalisedWest * c, totals._2 + next.position.lat * c, totals._3 + c)
            }
          }
          Cluster(id + "-" + segment, System.currentTimeMillis(), LatLng(lat / count, (lng / count) - 180d), count)
      }(collection.breakOut)
    } else {
      points
    }
  }

  /**
   * Group the positions into n2 boxes
   *
   * @param bbox The bounding box
   * @param positions The positions to group
   * @return The grouped positions
   */
  def groupNBoxes(bbox: BoundingBox, n: Int, positions: IndexedSeq[PointOfInterest]): Map[Int, IndexedSeq[PointOfInterest]] = {
    positions.groupBy { pos =>
      latitudeSegment(n, bbox.southWest.lat, bbox.northEast.lat, pos.position.lat) * n +
        longitudeSegment(n, bbox.southWest.lng, bbox.northEast.lng, pos.position.lng)
    }
  }

  /**
   * Find the segment that the point lies in in the given south/north range
   *
   * @return A number from 0 to n - 1
   */
  def latitudeSegment(n: Int, south: Double, north: Double, point: Double): Int = {
    // Normalise so that the southern most point is 0
    val range = north - south
    val normalisedPoint = point - south
    val segment = Math.floor(normalisedPoint * (n / range)).asInstanceOf[Int]
    if (segment >= n || segment < 0) {
      // The point was never in the given range.  Default to 0.
      0
    } else {
      segment
    }
  }

  /**
   * Find the segment that the point lies in in the given west/east range
   *
   * @return A number from 0 to n - 1
   */
  def longitudeSegment(n: Int, west: Double, east: Double, point: Double): Int = {
    // Normalise so that the western most point is 0, taking into account the 180 cut over
    val range = modPositive(east - west, 360)
    val normalisedPoint = modPositive(point - west, 360)
    val segment = Math.floor(normalisedPoint * (n / range)).asInstanceOf[Int]
    if (segment >= n || segment < 0) {
      // The point was never in the given range.  Default to 0.
      0
    } else {
      segment
    }
  }

  /**
   * Modulo function that always returns a positive number
   */
  def modPositive(x: Double, y: Int): Double = {
    val mod = x % y
    if (mod > 0) mod else mod + y
  }

  /**
   * Modulo function that always returns a positive number
   */
  def modPositive(x: Int, y: Int): Int = {
    val mod = x % y
    if (mod > 0) mod else mod + y
  }

}
