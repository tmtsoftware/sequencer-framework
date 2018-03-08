package tmt.sequencer

import akka.actor.typed.scaladsl.Behaviors.MutableBehavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import tmt.sequencer.models.SequencerMsg.ExternalSequencerMsg
import tmt.sequencer.models.ScriptRunnerMsg.ControlCommand
import tmt.sequencer.models.{ScriptRunnerMsg, SequencerMsg, SupervisorMsg}

class SupervisorBehavior(script: Script, sequencerRef: ActorRef[SequencerMsg], ctx: ActorContext[SupervisorMsg])
    extends MutableBehavior[SupervisorMsg] {

  private val scriptRunnerRef: ActorRef[ScriptRunnerMsg] =
    ctx.spawn(ScriptRunnerBehavior.behavior(script, sequencerRef), "scriptRunner")

  override def onMessage(msg: SupervisorMsg): Behavior[SupervisorMsg] = {
    msg match {
      case msg: ControlCommand       => scriptRunnerRef ! msg
      case msg: ExternalSequencerMsg => sequencerRef ! msg
      case _                         =>
    }
    this
  }
}

object SupervisorBehavior {
  def behavior(script: Script, sequencerRef: ActorRef[SequencerMsg]): Behavior[SupervisorMsg] = {
    Behaviors.mutable(ctx => new SupervisorBehavior(script, sequencerRef, ctx))
  }
}
