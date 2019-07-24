package esw.ocs.http

import csw.command.client.messages.ProcessSequenceError
import csw.params.commands.CommandResponse.SubmitResponse

sealed trait ProcessSequenceDto extends Product with Serializable

object ProcessSequenceDto {
  case class Success(response: SubmitResponse)    extends ProcessSequenceDto
  case class Failure(error: ProcessSequenceError) extends ProcessSequenceDto
}
