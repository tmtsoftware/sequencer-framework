package csw.messages.commands.matchers

import java.util.concurrent.CompletableFuture

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, OverflowStrategy}
import csw.messages.commands.matchers.MatcherResponses.{MatchCompleted, MatchFailed}
import csw.messages.framework.PubSub.Subscribe
import csw.messages.params.states.CurrentState
import csw.messages.scaladsl.ComponentCommonMessage.ComponentStateSubscription

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * The matcher uses the matching definition as provided by [[csw.messages.commands.matchers.StateMatcher]] to
 * match against the state generated by subscribing a source of that state
 *
 * @param currentStateSource source of the state which needs to be matched against the current state specified in state matcher
 * @param stateMatcher the matcher definition to execute
 */
class Matcher(
    currentStateSource: ActorRef[ComponentStateSubscription],
    stateMatcher: StateMatcher
)(implicit ec: ExecutionContext, mat: Materializer) {

  /**
   * Start the matching process
   *
   * @return the result of matching as a Future value of MatcherResponse
   */
  def start: Future[MatcherResponse] = currentStateF.transform {
    case Success(_)  ⇒ Success(MatchCompleted)
    case Failure(ex) ⇒ Success(MatchFailed(ex))
  }

  /**
   * Start the matching process from Java application
   *
   * @return the result of matching as a CompletableFuture of MatcherResponse
   */
  def jStart: CompletableFuture[MatcherResponse] = start.toJava.toCompletableFuture

  /**
   * Abort the stream of subscribed state in case matching is no longer required. Eg. when composing operations
   * in command execution, the matching was started before knowing the actual result of validation and the validation failed.
   *
   * {{{
      val matcherResponseF: Future[MatcherResponse] = matcher.start

      val eventualCommandResponse: Future[CommandResponse] = async {
        val initialResponse = await(assemblyComponent.oneway(setupWithMatcher))
        initialResponse match {
          case _: Accepted ⇒
            val matcherResponse = await(matcherResponseF)
            matcherResponse match {
              case MatchCompleted  ⇒ Completed(setupWithMatcher.runId)
              case MatchFailed(ex) ⇒ Error(setupWithMatcher.runId, ex.getMessage)
            }
          case invalid: Invalid ⇒
            matcher.stop()
            invalid
          case x ⇒ x
        }
   * }}}
   */
  def stop(): Unit = killSwitch.abort(MatchAborted(stateMatcher.prefix))

  /**
   * +----------------------------+
   * |  Source ActorRef           |                +---------------+   +----------------------+   +---------------------------+
   * |                            |                |  filter when  |   | wait for a specific  |   | Sink.head to complete the |
   * |  on materialization        |                |               |   |                      |   |                           |
   * |                            |~current state~>|  demand state |~~>| time to match demand |~~>| stream as soon as demand  |
   * |  subscribe to destination  |                |               |   |                      |   |                           |
   * |                            |                |  matches      |   | state                |   |  state matches            |
   * |  component's state         |                +---------------+   +----------------------+   +---------------------------+
   * |                            |
   * +----------------------------+
   *
   */
  private lazy val (killSwitch, currentStateF) = source
    .viaMat(KillSwitches.single)(Keep.right)
    .toMat(Sink.head)(Keep.both)
    .run()

  private def source =
    Source
      .actorRef[CurrentState](256, OverflowStrategy.fail)
      .mapMaterializedValue { ref ⇒
        currentStateSource ! ComponentStateSubscription(Subscribe(ref))
      }
      .filter(cs ⇒ cs.prefixStr == stateMatcher.prefix && stateMatcher.check(cs))
      .completionTimeout(stateMatcher.timeout.duration)
}
