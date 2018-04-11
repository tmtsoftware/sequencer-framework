package tmt.sequencer

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import ammonite.ops.{Path, RelPath}
import tmt.sequencer.git.{ScriptConfigs, ScriptRepo}
import tmt.sequencer.dsl.{CswServices, Script}
import tmt.sequencer.gateway.LocationService
import tmt.sequencer.models.SequencerMsg
import tmt.sequencer.rpc.api.{SequenceManager, SequenceProcessor}
import tmt.sequencer.rpc.server._

import scala.concurrent.duration.DurationDouble

class Wiring(sequencerId: String, observingMode: String, port: Option[Int], isProd: Boolean) {
  implicit lazy val timeout: Timeout           = Timeout(5.seconds)
  lazy implicit val system: ActorSystem        = ActorSystem("test")
  lazy implicit val materializer: Materializer = ActorMaterializer()
  import system.dispatcher

  lazy val scriptConfigs = new ScriptConfigs(system)
  lazy val repoDir: Path = if (isProd) Path(scriptConfigs.cloneDir) else ammonite.ops.pwd
  lazy val scriptRepo    = new ScriptRepo(scriptConfigs, locationService)
  lazy val path: Path    = repoDir / RelPath(scriptConfigs.scriptFactoryPath)

  lazy val sequencerRef: ActorRef[SequencerMsg] = system.spawn(SequencerBehaviour.behavior, "sequencer")
  lazy val sequencer                            = new Sequencer(sequencerRef, system)

  lazy val locationService = new LocationService
  lazy val engine          = new Engine
  lazy val cswServices     = new CswServices(sequencer, engine, locationService, sequencerId, observingMode)

  lazy val script: Script = ScriptImports.load(path).get(cswServices)

  lazy val sequenceManager: SequenceManager     = new SequenceManagerImpl(sequencerRef, script)
  lazy val sequenceProcessor: SequenceProcessor = new SequenceProcessorImpl(sequencerRef)
  lazy val routes                               = new Routes(sequenceProcessor, sequenceManager)
  lazy val rpcConfigs                           = new RpcConfigs(port)
  lazy val rpcServer                            = new RpcServer(rpcConfigs, routes)

  lazy val remoteRepl = new RemoteRepl(cswServices, sequencer, sequenceProcessor, sequenceManager, rpcConfigs)
}
