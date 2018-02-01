package tmt.sequencer

import akka.actor.Scheduler
import akka.typed.{ActorRef, ActorSystem}
import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import tmt.sequencer.Engine.{Command, Pull, Value}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationLong

class ScriptRunner(engine: ActorRef[Command], system: ActorSystem[_]) {
  implicit val timeout: Timeout = Timeout(1.hour)
  implicit val scheduler: Scheduler = system.scheduler

  import system.executionContext

  def run(): Unit = Future {
    val script = Script.fromFile("simple.ss")
    while (true) {
      script.run(pullNext().x)
    }
  }

  def pullNext(): Value = Await.result(engine ? Pull, timeout.duration)
}
