package conductr

import com.typesafe.conductr.bundlelib
import play.Application
import play.api.{Logger, Plugin}

/**
 * Uses ConductR's BundleLib to signal that the app is done initializing.
 */
class SignalReady(app: Application) extends Plugin {

  override def onStart(): Unit = {
    Logger.info("Signalling to ConductR that we've initialized properly")
    import com.typesafe.conductr.bundlelib.play.ConnectionContext.Implicits.defaultContext
    bundlelib.play.StatusService.signalStartedOrExit()
  }
}
