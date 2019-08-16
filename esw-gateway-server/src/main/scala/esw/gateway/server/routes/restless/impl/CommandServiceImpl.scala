package esw.gateway.server.routes.restless.impl

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.command.api.CurrentStateSubscription
import csw.params.commands.CommandResponse
import csw.params.core.states.CurrentState
import esw.gateway.server.routes.restless.api.CommandServiceApi
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import esw.gateway.server.routes.restless.messages.CommandAction.{Oneway, Submit, Validate}
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg.{InvalidComponent, InvalidMaxFrequency}
import esw.gateway.server.routes.restless.messages.RequestMsg.CommandMsg
import esw.gateway.server.routes.restless.messages.WebSocketMsg.CurrentStateSubscriptionCommandMsg
import esw.gateway.server.routes.restless.messages.{ErrorResponseMsg, WebSocketMsg}
import esw.http.core.utils.CswContext
import msocket.core.api.Payload

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.control.NonFatal

class CommandServiceImpl(cswCtx: CswContext) extends CommandServiceApi {

  import cswCtx._
  implicit val timeout: Timeout = Timeout(5.seconds)
  import actorRuntime.typedSystem.executionContext

  def process(commandMsg: CommandMsg): Future[Either[ErrorResponseMsg, CommandResponse]] = {
    import commandMsg._
    componentFactory
      .commandService(componentName, componentType)
      .flatMap { commandService =>
        action match {
          case Oneway   => commandService.oneway(command)
          case Submit   => commandService.submit(command)
          case Validate => commandService.validate(command)
        }
      }
      .map(Right(_))
      .recover {
        case NonFatal(ex) => Left(InvalidComponent(ex.getMessage))
      }
  }

  def queryFinal(queryCommandMsg: WebSocketMsg.QueryCommandMsg): Future[Either[ErrorResponseMsg, CommandResponse]] = {
    import queryCommandMsg._
    componentFactory
      .commandService(componentName, componentType)
      .flatMap(_.queryFinal(runId)(Timeout(100.hours)))
      .map(Right(_))
      .recover {
        case NonFatal(ex) => Left(InvalidComponent(ex.getMessage))
      }
  }

  override def subscribeCurrentState(currentStateSubscriptionCommandMsg: WebSocketMsg.CurrentStateSubscriptionCommandMsg): Unit =
    ???
}
