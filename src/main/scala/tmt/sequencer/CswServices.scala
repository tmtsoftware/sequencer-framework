package tmt.sequencer

import tmt.sequencer.FutureExt.RichFuture
import tmt.sequencer.models.{Command, CommandResult, Step}

import scala.concurrent.ExecutionContext

class CswServices(locationService: LocationService, sequencer: Sequencer)(implicit ec: ExecutionContext) {
  def setup(componentName: String, command: Command): CommandResult = {
    val assembly = locationService.resolve(componentName)
    //convert command into Controlcommand Setup
    assembly.submit(command)
  }.await

  def observe(componentName: String, command: Command): CommandResult = {
    val assembly = locationService.resolve(componentName)
    //convert command into Controlcommand Observe
    assembly.submit(command)
  }.await

  def setupAndSubscribe(componentName: String, command: Command): CommandResult = {
    val assembly = locationService.resolve(componentName)
    //convert command into Controlcommand Observe
    assembly.submit(command)
  }.await

  def split(params: List[Int]): (List[Int], List[Int]) = params.partition(_ % 2 != 0)

  def hasNext: Boolean = sequencer.hasNext
  def next: Step       = sequencer.next
}
