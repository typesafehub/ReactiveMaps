package backend

import akka.actor.ActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.ExtendedActorSystem
import scala.concurrent.duration._
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit

class Settings(config: Config) extends Extension {
  /**
   * The maximum zoom depth for regions.  The concrete regions will sit at this depth, summary regions will sit above
   * that.
   */
  val MaxZoomDepth = config.getInt("reactiveMaps.maxZoomDepth")

  /**
   * The maximum number of regions that can be subscribed to.
   *
   * This is enforced automatically by selecting the deepest zoom depth for a given bounding box that is covered by
   * this number of regions or less.
   */
  val MaxSubscriptionRegions = config.getInt("reactiveMaps.maxSubscriptionRegions")

  /**
   * The number of points that need to be in a region/summary region before it decides to cluster them.
   */
  val ClusterThreshold = config.getInt("reactiveMaps.clusterThreshold")

  /**
   * The dimension depth at which to cluster.
   *
   * A region will be clustered into the square of this number boxes.
   */
  val ClusterDimension = config.getInt("reactiveMaps.clusterDimension")

  /**
   * The interval at which each region should generate and send its summaries.
   */
  val SummaryInterval = config.getDuration("reactiveMaps.summaryInterval", TimeUnit.MILLISECONDS).milliseconds

  /**
   * The interval after which user positions and cluster data should expire.
   */
  val ExpiryInterval = config.getDuration("reactiveMaps.expiryInterval", TimeUnit.MILLISECONDS).milliseconds

  /**
   * The interval at which subscribers should batch their points to send to clients.
   */
  val SubscriberBatchInterval = config.getDuration("reactiveMaps.subscriberBatchInterval", TimeUnit.MILLISECONDS).milliseconds

  /**
   * Geospatial functions.
   */
  val GeoFunctions = new GeoFunctions(this)

  /**
   * Whether this node should run the bots it knows about.
   */
  val BotsEnabled = config.getBoolean("reactiveMaps.bots.enabled")

  /**
   * How many bots to create in total
   */
  val TotalNumberOfBots = config.getInt("reactiveMaps.bots.totalNumberOfBots")
}

/**
 * The settings for this application.
 */
object Settings extends ExtensionId[Settings] with ExtensionIdProvider {

  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) =
    new Settings(system.settings.config)

  override def get(system: ActorSystem): Settings = super.get(system)
}