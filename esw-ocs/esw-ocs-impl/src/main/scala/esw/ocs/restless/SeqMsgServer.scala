package esw.ocs.restless

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.{HttpApp, Route}
import csw.location.client.HttpCodecs
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.core.Sequencer
import esw.ocs.dsl.Script
import esw.ocs.restless.SeqMsg.{
  Abort,
  Add,
  AddBreakpoint,
  Available,
  Delete,
  EditorMsg,
  GetPreviousSequence,
  GetSequence,
  InsertAfter,
  Pause,
  Prepend,
  RemoveBreakpoint,
  Replace,
  Reset,
  Resume,
  SeqSignal,
  Shutdown
}

class SeqMsgServer(sequencer: SequencerImplStub, script: Script)(implicit _actorSystem: ActorSystem[_])
    extends HttpApp
    with SeqMsgCodecs
    with HttpCodecs
    with OcsFrameworkCodecs {

  override implicit def actorSystem: ActorSystem[_] = _actorSystem

  override protected def routes: Route = post {
    path("signals") {
      entity(as[SeqSignal]) {
        case Shutdown => complete(script.executeShutdown())
        case Abort    => complete(script.executeAbort())
      }
    } ~
    path("editor-actions") {
      entity(as[EditorMsg]) {
        case Available                 => complete(sequencer.isAvailable)
        case GetSequence               => complete(sequencer.getSequence)
        case GetPreviousSequence       => complete(sequencer.getPreviousSequence)
        case Add(commands)             => complete(sequencer.add(commands))
        case Prepend(commands)         => complete(sequencer.prepend(commands))
        case Replace(id, commands)     => complete(sequencer.replace(id, commands))
        case InsertAfter(id, commands) => complete(sequencer.insertAfter(id, commands))
        case Delete(ids)               => complete(sequencer.delete(ids))
        case AddBreakpoint(id)         => complete(sequencer.addBreakpoint(id))
        case RemoveBreakpoint(id)      => complete(sequencer.removeBreakpoint(id))
        case Pause                     => complete(sequencer.pause)
        case Resume                    => complete(sequencer.resume)
        case Reset                     => complete(sequencer.reset())
      }
    }
  }
}
