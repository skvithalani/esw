package esw.ocs.restless

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.aas.http.AuthorizationPolicy.ClientRolePolicy
import csw.aas.http.SecurityDirectives
import csw.location.client.HttpCodecs
import csw.location.client.scaladsl.HttpLocationServiceFactory
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

  implicit val system = _actorSystem
  implicit val mat    = ActorMaterializer()
  val locationService = HttpLocationServiceFactory.makeLocalClient

  val directives = SecurityDirectives(locationService)

  import directives._

  override implicit def actorSystem: ActorSystem[_] = _actorSystem

  override protected def routes: Route = post {
    path("signals") {
      entity(as[SeqSignal]) {
        case Shutdown => complete(script.executeShutdown())
        case Abort    => complete(script.executeAbort())
      }
    } ~
    authenticate { token =>
      path("editor-actions") {
        entity(as[EditorMsg]) {
          case Available =>
            authorize1(ClientRolePolicy(token.userOrClientName), token) {
              complete(sequencer.isAvailable)
            }
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
}
