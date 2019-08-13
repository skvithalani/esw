package esw.ocs.restless

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.location.client.HttpCodecs
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.SequenceEditor
import esw.ocs.api.codecs.OcsFrameworkCodecs
import esw.ocs.api.models.StepList
import esw.ocs.api.models.messages.EditorResponse
import esw.ocs.restless.SeqMsg._
import io.bullet.borer.{Decoder, Encoder}

import scala.async.Async._
import scala.concurrent.Future

class SeqMsgClient(_actorSystem: ActorSystem[_])
    extends SeqMsgCodecs
    with HttpCodecs
    with OcsFrameworkCodecs
    with SequenceEditor {

  override implicit val actorSystem: ActorSystem[_] = _actorSystem

  import actorSystem.executionContext
  implicit val untypedSystem: actor.ActorSystem = actorSystem.toUntyped
  implicit val mat: Materializer                = ActorMaterializer()

  private val editorActionsUri = s"http://localhost:6000/editor-actions"

  def dd[Req: Encoder, Res: Decoder](req: Req): Future[Res] = async {
    val requestEntity = await(Marshal(req).to[RequestEntity])
    val request       = HttpRequest(HttpMethods.POST, uri = editorActionsUri, entity = requestEntity)
    val response      = await(Http().singleRequest(request))
    await(Unmarshal(response.entity).to[Res])
  }

  override def status: Future[StepList] = { println("status"); dd[EditorMsg, StepList](GetSequence) }

  override def isAvailable: Future[Boolean] = ???

  override def add(commands: List[SequenceCommand]): Future[EditorResponse] = ???

  override def prepend(commands: List[SequenceCommand]): Future[EditorResponse] = ???

  override def replace(id: Id, commands: List[SequenceCommand]): Future[EditorResponse] = ???

  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[EditorResponse] = ???

  override def delete(id: Id): Future[EditorResponse] = ???

  override def pause: Future[EditorResponse] = ???

  override def resume: Future[EditorResponse] = ???

  override def addBreakpoint(id: Id): Future[EditorResponse] = ???

  override def removeBreakpoint(id: Id): Future[EditorResponse] = ???

  override def reset(): Future[EditorResponse] = ???

  override def shutdown(): Future[EditorResponse] = ???

  override def abort(): Future[EditorResponse] = ???
}
