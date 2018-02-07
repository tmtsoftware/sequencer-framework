package tmt.sequencer

import org.scalatest.Matchers
import Dsl._
import scala.concurrent.ExecutionContext.Implicits.global

class CommandServiceTest extends org.scalatest.FunSuite with Matchers {

  def x: Int = {
    println("blocking")
    10
  }

  test("dd") {
    val dsl = new CommandService(null)
    println(par(x, x))
  }
}