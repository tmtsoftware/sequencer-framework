import tmt.sequencer.ScriptImports._
import tmt.sequencer.models.EngineMsg.SequencerEvent

import scala.concurrent.duration.DurationDouble

class IrisDarkNight(cs: CswServices) extends Script(cs) {

  override def observingMode = "DarkNight"

  var eventCount = 0
  var commandCount = 0

  val subscription = cs.subscribe("iris") { event =>
    eventCount = eventCount + 1
    println(s"received: ------------------> event=${event.value} on key=${event.key}")
  }

  val cancellable = cs.publish(every = 5.seconds) {
    val totalCount = eventCount + commandCount
    SequencerEvent("iris-metadata", totalCount.toString)
  }

  override def execute(command: Command): Future[CommandResults] = spawn {
    commandCount += 1
    if (command.name == "setup-iris") {
      val commandResult = cs.setup("iris-assembly1", command).await
      val commandFailed = commandResult.isInstanceOf[CommandResult.Failed]

      val commandResults = if (commandFailed) {
        CommandResults.from(cs.setup("iris-assembly2", command).await)
      } else {
        CommandResults(
          par(
            cs.setup("iris-assembly3", command),
            cs.setup("iris-assembly4", command)
          ).await
        )
      }

      val finalResults = commandResults.addResult(commandResult)
      println(s"final result = $finalResults")
      finalResults
    } else {
      println(s"unknown command=$command")
      CommandResults.empty
    }
  }

  override def onShutdown(): Future[Unit] = spawn {
    subscription.shutdown()
    cancellable.cancel()
    println("shutdown")
  }
}
