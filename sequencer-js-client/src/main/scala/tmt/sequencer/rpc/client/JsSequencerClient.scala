package tmt.sequencer.rpc.client

import io.circe.generic.auto._
import chameleon.ext.circe._

import covenant.http.HttpClient
import tmt.sequencer.api.{SequenceEditor, SequenceFeeder}

import scala.scalajs.js.annotation._

class JsSequencerClient(baseUri: String) {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val client = HttpClient[String](baseUri)

  val sequenceFeeder: SequenceFeeder = client.wire[SequenceFeeder]
  val sequenceEditor: SequenceEditor = client.wire[SequenceEditor]
}

@JSExportTopLevel("AA")
@JSExportAll
object AA {
  def dd(): String = "123"
}
