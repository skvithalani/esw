package esw.http.core.commons

import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import io.bullet.borer.compat.akka._
import io.bullet.borer.{Encoder, Json}

import scala.concurrent.Future

object ToMessage {
  implicit class ValueToMessage[T: Encoder](x: T) {
    def toText: String     = Json.encode(x).to[ByteString].result.utf8String
    def toTextMessage: TextMessage = TextMessage(toText)
  }
  implicit class FlowToMessageFlow[T: Encoder](x: Future[T]) {
    def toTextMessageFlow: Flow[Message, TextMessage, NotUsed] = Flow.fromSinkAndSource(Sink.ignore, Source.fromFuture(x).map(_.toTextMessage))
  }
  implicit class SourceToMessage[T: Encoder, Mat](stream: Source[T, Mat]) {
    def toTextMessage: TextMessage = TextMessage(stream.map(_.toText))
  }
}
