package tmt.sequencer

object ScriptRunner {
  def run(sequencerId: String, observingMode: String, isProd: Boolean): Unit = {
    val wiring = new Wiring(sequencerId, observingMode, isProd)
    import wiring._
    if (isProd) {
      scriptRepo.cloneRepo()
    }
    engine.start(sequencer, script)
    rpcServer.start(9090)
    supervisorRef
    remoteRepl.server().start()
  }
}
