package tmt.sequencer

import akka.actor.typed.ActorRef
import ammonite.sshd._
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator
import tmt.sequencer.gateway.CswServices
import tmt.sequencer.models.SupervisorMsg.ControlCommand
import tmt.sequencer.models.{Command, Id, SupervisorMsg}
import tmt.sequencer.core.Sequencer

class RemoteRepl(commandService: CswServices, sequencer: Sequencer, supervisor: ActorRef[SupervisorMsg]) {

  def server() = new SshdRepl(
    SshServerConfig(
      address = "localhost", // or "0.0.0.0" for public-facing shells
      port = 22222, // Any available port
      passwordAuthenticator = Some(AcceptAllPasswordAuthenticator.INSTANCE) // or publicKeyAuthenticator
    ),
    predef = """
         |def setFlags() = repl.compiler.settings.Ydelambdafy.value = "inline"
         |import scala.concurrent.duration.Duration
         |import scala.concurrent.{Await, Future}
         |implicit class RichFuture[T](val f: Future[T]) {
         |  def get: T = Await.result(f, Duration.Inf)
         |}
      """.stripMargin,
    replArgs = Seq(
      "cs"             -> commandService,
      "sequencer"      -> sequencer,
      "Command"        -> Command,
      "Id"             -> Id,
      "supervisor"     -> supervisor,
      "controlCommand" -> ControlCommand
    )
  )
}
