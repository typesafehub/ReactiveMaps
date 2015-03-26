package conductr

import com.typesafe.conductr.bundlelib.akka.ClusterProperties
import play.Application
import play.api.{Logger, Plugin}

/**
 * Uses ConductR's BundleLib for the cluster's seed-node initialization.
 * This allows the Akka cluster to discover the right nodes within a dynamically managed ConductR cluster.
 *
 * This plugin MUST be started before the Akka cluster is initialized.
 */
class ConductrManagedCluster(app: Application) extends Plugin {
  override def onStart(): Unit = {
    Logger.info("Initializing akka cluster properties using ConductR")
    ClusterProperties.initialize() // prepares seed node env variables based on information from ConductR
  }
}
