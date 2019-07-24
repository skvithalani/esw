package esw.ocs.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import csw.command.client.messages.ProcessSequenceError
import csw.command.client.messages.ProcessSequenceError.DuplicateIdsFound
import csw.location.client.HttpCodecs
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandResponse, Sequence}
import csw.params.core.models.Id
import esw.ocs.core.Sequencer
import esw.ocs.http.RichProcessSequenceDto.RichProcessSequence

import scala.concurrent.{ExecutionContext, Future}

class Route(sequencer: Sequencer)(implicit _actorSystem: ActorSystem[_]) extends HttpCodecs with Codecs {

  implicit val ec: ExecutionContext = _actorSystem.executionContext

  def handleEither[E, R: ToResponseMarshaller](x: Either[E, R])(f: E => StatusCode)(
      implicit ev: ToResponseMarshaller[(StatusCode, E)]
  ): ToResponseMarshallable = {
    x match {
      case Right(v) => v
      case Left(z)  => f(z) -> z
    }
  }

  def handler(x: ProcessSequenceError): StatusCode = x match {
    case ProcessSequenceError.DuplicateIdsFound           => StatusCodes.BadRequest
    case ProcessSequenceError.ExistingSequenceIsInProcess => StatusCodes.InternalServerError
  }

  val route =
    post {
      path("process-sequence") {
        entity(as[Sequence]) { sequence =>
          val ff = sequencer
            .processSequence(sequence)
            .map(x => {
              handleEither(x.response)(handler)
            })
          val response: Future[ToResponseMarshallable] = sequencer
            .processSequence(sequence)
            .map[ToResponseMarshallable] { x =>
              x.response match {
                case Right(x) => StatusCodes.Accepted   -> x
                case Left(x)  => StatusCodes.BadRequest -> x
              }
            }
          complete(response)
//          complete(ff)
        }
      } ~
      path("get-sequence") {
        complete(sequencer.getSequence)
      } ~
      path("available") {
        complete(sequencer.isAvailable)
      } ~
      path("ping") {
        complete("pong")
      }
    }

  override implicit def actorSystem: ActorSystem[_] = _actorSystem
}
