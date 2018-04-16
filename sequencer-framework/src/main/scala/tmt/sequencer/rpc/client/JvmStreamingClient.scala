package tmt.sequencer.rpc.client

import boopickle.Default._
import chameleon.ext.boopickle._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import covenant.ws.WsClient
import monix.reactive.Observable
import mycelium.client.WebsocketClientConfig
import tmt.sequencer.api.Streaming

import scala.concurrent.Future

import boopickle.Default._
import chameleon.ext.boopickle._
import java.nio.ByteBuffer

class JvmStreamingClient(baseUri: String)(implicit system: ActorSystem) {
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  import materializer.executionContext

  private val config               = WebsocketClientConfig()
  private val wsClient             = WsClient[ByteBuffer, Int, String](s"ws://0.0.0.0:9090/ws", config)
  val streaming: Streaming[Future] = wsClient.sendWithDefault.wire[Streaming[Future]]

  val events: Observable[List[Int]] = wsClient.observable.event
}