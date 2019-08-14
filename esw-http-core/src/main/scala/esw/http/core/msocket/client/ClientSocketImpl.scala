package esw.http.core.msocket.client

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage, WebSocketRequest}
import akka.stream.scaladsl.Source
import esw.http.core.msocket.api.{Encoding, Payload}
import io.bullet.borer.{Decoder, Encoder}

class ClientSocketImpl[Req: Encoder](baseUri: String, encoding: Encoding)(implicit actorSystem: ActorSystem)
    extends ClientSocket[Req] {

  private val setup = new ClientSocketSetup(WebSocketRequest(s"$baseUri/${encoding.Name}"))

  override def requestStream[Res: Decoder: Encoder](request: Req): Source[Res, NotUsed] = {
    setup
      .request(encoding.strictMessage(Payload(request)))
      .collect {
        case BinaryMessage.Strict(data) if !encoding.isBinary => encoding.decodeBinary(data).value
        case TextMessage.Strict(text) if !encoding.isBinary   => encoding.decodeText(text).value
      }
  }
}
