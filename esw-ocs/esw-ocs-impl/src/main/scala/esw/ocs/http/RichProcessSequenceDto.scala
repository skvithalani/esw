package esw.ocs.http

import csw.command.client.messages.ProcessSequenceResponse

import scala.concurrent.{ExecutionContext, Future}

object RichProcessSequenceDto {
  implicit class RichProcessSequence(val processSequenceResponse: Future[ProcessSequenceResponse])(
      implicit ec: ExecutionContext
  ) {
    def toDto: Future[ProcessSequenceDto] = {
      processSequenceResponse.map { p =>
        p.response match {
          case Left(error)           => ProcessSequenceDto.Failure(error)
          case Right(submitResponse) => ProcessSequenceDto.Success(submitResponse)
        }
      }
    }
  }
}
