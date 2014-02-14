package actors

import play.api.Application
import akka.actor._
import play.api.libs.iteratee._
import play.api.mvc.{RequestHeader, WebSocket}
import play.api.libs.concurrent.Akka
import akka.actor.Terminated
import akka.actor.SupervisorStrategy.Stop
import scala.reflect.ClassTag

/**
 * Integration between Play WebSockets and actors
 */
object WebSocketActor {

  /**
   * Create a WebSocket that will pass messages to/from the actor created by the given props
   *
   * The passed in function allows the creation of an actor based on the request.  It should return a function that
   * will take the upstream actor, and return props for creating a new actor to receive WebSocket messages.  Messages
   * can be sent to the WebSocket by sending messages to the upstream actor, and the WebSocket can be closed either
   * by stopping the upstream handler, or by this actor stopping itself.
   *
   * @param f A function that takes the request header, and returns a function of the upstream actor to send
   *          messages to properties to receive messages from.
   */
  def actorOf[M](f: RequestHeader => ActorRef => Props)(implicit frameFormatter: WebSocket.FrameFormatter[M],
                                            app: Application, messageType: ClassTag[M]): WebSocket[M] = {
    WebSocket[M] { request =>
      (enumerator, iteratee) =>
        WebSocketsExtension(Akka.system).actor ! WebSocketsActor.Connect(request.id, enumerator, iteratee, f(request))
    }
  }

  /**
   * The actor that supervises and handles all messages to/from the WebSocket actor.
   */
  private class WebSocketActorSupervisor[M](enumerator: Enumerator[M], iteratee: Iteratee[M, Unit], createHandler: ActorRef => Props)
                                   (implicit messageType: ClassTag[M]) extends Actor {

    import context.dispatcher

    // The actor to handle the WebSocket
    val webSocketActor = context.watch(context.actorOf(createHandler(self), "handler"))

    // Use a broadcast enumerator to imperatively push messages into the WebSocket
    val channel = {
      val (enum, chan) = Concurrent.broadcast[M]
      // Ensure we feed EOF into the iteratee when done, to ensure that the WebSocket gets closed
      enum |>>> iteratee
      chan
    }

    // Use a foreach iteratee to consume the WebSocket and feed it into the Actor
    (enumerator |>> Iteratee.foreach { msg =>
      webSocketActor ! msg
    }).onComplete {
      // When the WebSocket is complete, either due to an error or not, shutdown
      case _ => self ! PoisonPill
    }

    def receive = {
      case _: Terminated =>
        // Child has terminated, close the WebSocket. We don't have to stop ourselves, since this will be done when the
        // channel closes, which will cause the enumerator above to complete feeding into the iteratee, where we have
        // a callback set to stop ourselves
        channel.end()
      // A message of the type that we're handling has been received
      case messageType(a) => channel.push(a)
    }

    // Stop the child if it gets an exception
    override val supervisorStrategy = OneForOneStrategy() {
      case _ => Stop
    }

    override def postStop() = {
      // In the normal shutdown case, this will already have been called, that's ok, channel.end() is a no-op in that
      // case.  This does however handle the case where this supervisor crashes, or when it's stopped externally.
      channel.end()
    }
  }

  private object WebSocketsActor {

    /**
     * Connect an actor to the WebSocket on the end of the given enumerator/iteratee.
     *
     * @param requestId The requestId. Used to name the actor.
     * @param enumerator The enumerator to send messages to.
     * @param iteratee The iteratee to consume messages from.
     * @param createHandler A function that creates a handler to handle the websocket, given an actor to send messages
     *                      to.
     * @param messageType The type of message this WebSocket deals with.
     */
    case class Connect[M](requestId: Long, enumerator: Enumerator[M], iteratee: Iteratee[M, Unit],
                          createHandler: ActorRef => Props)(implicit val messageType: ClassTag[M])
  }

  /**
   * The actor responsible for creating all web sockets
   */
  private class WebSocketsActor extends Actor {
    import WebSocketsActor._

    def receive = {
      case c @ Connect(requestId, enumerator, iteratee, createHandler) =>
        implicit val mt = c.messageType
        context.actorOf(Props(new WebSocketActorSupervisor(enumerator, iteratee, createHandler)), requestId.toString)
    }
  }

  /**
   * The extension for managing WebSockets
   */
  private object WebSocketsExtension extends ExtensionId[WebSocketsExtension] {
    def createExtension(system: ExtendedActorSystem) = {
      new WebSocketsExtension(system.actorOf(Props(new WebSocketsActor), "websockets"))
    }
  }

  private class WebSocketsExtension(val actor: ActorRef) extends Extension


}
