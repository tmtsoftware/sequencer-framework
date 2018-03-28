package tmt.sequencer.dsl

import java.util.concurrent.Executors

import org.tmt.macros.AsyncMacros
import tmt.sequencer.models.CommandResult

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.language.experimental.macros
import scala.language.implicitConversions

trait Strand {
  protected implicit val ec: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  def par(fs: Future[CommandResult]*): Future[List[CommandResult]] = Future.sequence(fs.toList)

  implicit class RichF[T](t: Future[T]) {
    final def await: T = macro AsyncMacros.await
  }

  def spawn[T](body: => T)(implicit ec: ExecutionContext): Future[T] = macro AsyncMacros.async[T]

  private[sequencer] def shutdownEc(): Unit = ec.shutdown()
}

object Strand extends Strand