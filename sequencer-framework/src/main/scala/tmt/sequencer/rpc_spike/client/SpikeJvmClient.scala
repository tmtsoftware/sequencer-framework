package tmt.sequencer.rpc_spike.client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import covenant.ws.WsClient
import monix.reactive.Observable
import mycelium.client.WebsocketClientConfig
import tmt.sequencer.rpc_spike.{Advanced, Basic, Streaming}

import scala.concurrent.Future

object SpikeJvmClient {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem             = ActorSystem("client")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    import monix.execution.Scheduler.Implicits.global

    import sloth._
    import boopickle.Default._
    import chameleon.ext.boopickle._
    import java.nio.ByteBuffer
    import covenant.http._
    import ByteBufferImplicits._

    val client: Client[ByteBuffer, Future, ClientException] = HttpClient[ByteBuffer]("http://0.0.0.0:9090/api")
    val basic: Basic                                        = client.wire[Basic]
    val advanced: Advanced                                  = client.wire[Advanced]

    basic.increment(10).foreach { num =>
      println(s"Got response: $num")
    }

    advanced.square(10).foreach { num =>
      println(s"Got response: $num")
    }

    /////////////////////////
    val config                                                               = WebsocketClientConfig()
    val wsClient: WsClient[ByteBuffer, Future, Int, String, ClientException] = WsClient(s"ws://0.0.0.0:9090/ws", config)
    val streaming: Streaming[Future]                                         = wsClient.sendWithDefault.wire[Streaming[Future]]

    wsClient.observable.event.flatMap(xs => Observable.fromIterable(xs)).foreach(println)

    streaming.from(78).onComplete { res =>
      println(s"Got response: $res")
    }

  }
}
