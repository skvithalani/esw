package esw.ocs.restless

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.models.StepList
import esw.ocs.api.models.messages.EditorResponse

import scala.concurrent.Future

class SequencerImplStub {
  def getSequence: Future[StepList]         = { Future.successful(StepList.empty) }
  def getPreviousSequence: Future[StepList] = { Future.successful(StepList.empty) }

  def isAvailable: Future[Boolean] = ???

  def add(commands: List[SequenceCommand]): Future[EditorResponse] = ???

  def prepend(commands: List[SequenceCommand]): Future[EditorResponse] = ???

  def replace(id: Id, commands: List[SequenceCommand]): Future[EditorResponse] = ???

  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[EditorResponse] = ???

  def delete(id: Id): Future[EditorResponse] = ???

  def pause: Future[EditorResponse] = ???

  def resume: Future[EditorResponse] = ???

  def addBreakpoint(id: Id): Future[EditorResponse] = ???

  def removeBreakpoint(id: Id): Future[EditorResponse] = ???

  def reset(): Future[EditorResponse] = ???

  def shutdown(): Future[EditorResponse] = ???

  def abort(): Future[EditorResponse] = ???
}
