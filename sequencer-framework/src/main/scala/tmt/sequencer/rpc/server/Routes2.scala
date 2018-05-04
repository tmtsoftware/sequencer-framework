package tmt.sequencer.rpc.server

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import scalapb.json4s.JsonFormat
import sequencer_protobuf.command.{PbCommandList, PbMyJson}
import tmt.sequencer.api.SequenceFeeder
import tmt.sequencer.models.{AggregateResponse, CommandList, Msg}
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

class Routes2(sequenceFeeder: SequenceFeeder)(implicit ec: ExecutionContext) {
  val route =
    post {
      path("json-route") {
        entity(as[Msg]) { msg =>
          val response: Future[Msg] = sequenceFeeder.testJsonApi(msg)
          onComplete(response) { done =>
            complete(response)
          }
        }
      } ~
      path("pbjson-route") {
        //hardcoded json string
        val proto: PbMyJson = JsonFormat.fromJsonString[PbMyJson]("""{"value": "testval"}""")
        println("**********" + proto.toString)
        val response: Future[PbMyJson] = sequenceFeeder.testPbWithJsonApi(proto)
        onComplete(response) { done =>
          complete("complete")
        }

      } ~
      path("pbjson-chimney-route") {
        //hardcoded json string
        val proto: PbMyJson = JsonFormat.fromJsonString[PbMyJson]("""{"value": "testval"}""")
        val msg             = proto.transformInto[Msg]
        println("**********" + msg.toString)
        val response: Future[Msg] = sequenceFeeder.testJsonApi(msg)
        onComplete(response) { done =>
          complete(response)
        }

      } ~
      path("feed") {
        //hardcoded json string
        val proto: PbCommandList = JsonFormat.fromJsonString[PbCommandList]("""{
                                                                        "commands": [
                                                                          {
                                                                            "id": "1",
                                                                            "name": "setup-iris",
                                                                            "params": [1,2]
                                                                          }
                                                                          ]
                                                                      }""".stripMargin)
        val commandList          = proto.transformInto[CommandList]
        println("**********" + commandList.toString)
        val response: Future[AggregateResponse] = sequenceFeeder.feed(commandList)
        onComplete(response) { done =>
          complete("complete")
        }

      }
    }
}
