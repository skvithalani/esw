package esw.ocs.restless

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id

sealed trait SeqMsg
object SeqMsg {
  sealed trait SeqSignal extends SeqMsg
  sealed trait EditorMsg extends SeqMsg

  final case object Shutdown extends SeqSignal
  final case object Abort    extends SeqSignal

  final case object Available                                           extends EditorMsg
  final case object GetSequence                                         extends EditorMsg
  final case object GetPreviousSequence                                 extends EditorMsg
  final case class Add(commands: List[SequenceCommand])                 extends EditorMsg
  final case class Prepend(commands: List[SequenceCommand])             extends EditorMsg
  final case class Replace(id: Id, commands: List[SequenceCommand])     extends EditorMsg
  final case class InsertAfter(id: Id, commands: List[SequenceCommand]) extends EditorMsg
  final case class Delete(ids: Id)                                      extends EditorMsg
  final case class AddBreakpoint(id: Id)                                extends EditorMsg
  final case class RemoveBreakpoint(id: Id)                             extends EditorMsg
  final case object Pause                                               extends EditorMsg
  final case object Resume                                              extends EditorMsg
  final case object Reset                                               extends EditorMsg
}
