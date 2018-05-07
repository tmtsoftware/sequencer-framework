package tmt.sequencer.models

import play.api.libs.json._
import tmt.sequencer.models.StepStatus.{Finished, InFlight, Pending}

case class Step(command: Command, status: StepStatus, hasBreakpoint: Boolean) {
  def id: Id              = command.id
  def isPending: Boolean  = status == StepStatus.Pending
  def isFinished: Boolean = status == StepStatus.Finished

  def addBreakpoint(): Step    = if (isPending) copy(hasBreakpoint = true) else this
  def removeBreakpoint(): Step = copy(hasBreakpoint = false)

  def withStatus(newStatus: StepStatus): Step = {
    (status, newStatus) match {
      case (Pending, InFlight)  => copy(status = newStatus)
      case (InFlight, Finished) => copy(status = newStatus)
      case _                    => this
    }
  }
}

object Step {
  def from(command: Command)                    = Step(command, StepStatus.Pending, hasBreakpoint = false)
  def from(commands: List[Command]): List[Step] = commands.map(command => from(command))
}

sealed trait StepStatus

object StepStatus {
  case object Pending  extends StepStatus
  case object InFlight extends StepStatus
  case object Finished extends StepStatus
}

case class Id(value: String) extends AnyVal {
  override def toString: String = value
}

object Id {
  implicit val format: OFormat[Id] = Json.format[Id]
}

case class Command(id: Id, name: String, params: Seq[Int]) {
  def withId(id: Id): Command         = copy(id = id)
  def withName(name: String): Command = copy(name = name)
}

object Command {
  def root(id: Id, name: String, params: List[Int]) = Command(id, name, params)
  implicit val format: OFormat[Command]             = Json.format[Command]
}

case class CommandList(commands: Seq[Command]) {
  def add(others: Command*): CommandList   = CommandList(commands ++ others)
  def add(other: CommandList): CommandList = CommandList(commands ++ other.commands)
}
object CommandList {
  def from(commands: Command*): CommandList = CommandList(commands.toList)
  def empty: CommandList                    = CommandList(Nil)
  implicit val format: OFormat[CommandList] = Json.format[CommandList]
}

sealed trait CommandResponse {
  def typeName: String
  def id: Id
  def value: String
}

object CommandResponse {
  private val successType = classOf[Success].getSimpleName
  private val failedType  = classOf[Failed].getSimpleName

  case class Success(id: Id, value: String, typeName: String = Success.getClass.getSimpleName) extends CommandResponse
  case class Failed(id: Id, value: String, typeName: String = Failed.getClass.getSimpleName)   extends CommandResponse

  implicit val format: Format[CommandResponse] = new Format[CommandResponse] {
    override def writes(response: CommandResponse): JsValue = {
      JsObject(
        Seq(
          "type"  → JsString(response.typeName),
          "id"    → Id.format.writes(response.id),
          "value" → JsString(response.value)
        )
      )
    }

    override def reads(json: JsValue): JsResult[CommandResponse] = {
      json match {
        case JsObject(fields) =>
          (fields("type"), fields("id"), fields("value")) match {
            case (JsString(typeName), id, JsString(value)) =>
              typeName match {
                case `successType` =>
                  Success(id.as[Id], value, typeName).asInstanceOf[JsResult[CommandResponse]]
                case `failedType` =>
                  Success(id.as[Id], value, typeName).asInstanceOf[JsResult[CommandResponse]]
              }
          }
      }
    }
  }

}
case class AggregateResponse(childResponses: Set[CommandResponse]) {
  def ids: Set[Id]                                                 = childResponses.map(_.id)
  def add(commandResponses: CommandResponse*): AggregateResponse   = copy(childResponses ++ commandResponses.toSet)
  def add(maybeResponse: Set[CommandResponse]): AggregateResponse  = copy(childResponses ++ maybeResponse)
  def add(aggregateResponse: AggregateResponse): AggregateResponse = copy(childResponses ++ aggregateResponse.childResponses)
  def markSuccessful(commands: Command*): AggregateResponse = add {
    commands.map(command => CommandResponse.Success(id = command.id, value = "all children are done")).toSet[CommandResponse]
  }
  def markSuccessful(maybeCommand: Option[Command]): AggregateResponse = markSuccessful(maybeCommand.toList: _*)
}

object AggregateResponse extends AggregateResponse(Set.empty) {
  implicit val format: OFormat[AggregateResponse] = Json.format[AggregateResponse]
}

case class Msg(value: String) extends AnyVal {
  override def toString: String = value
}

object Msg {
  implicit val format: OFormat[Msg] = Json.format[Msg]
}
