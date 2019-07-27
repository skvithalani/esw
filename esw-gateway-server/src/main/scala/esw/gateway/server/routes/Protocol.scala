package esw.gateway.server.routes

import akka.NotUsed
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{DelayOverflowStrategy, Materializer}
import esw.gateway.server.routes.Protocol.{
  BitInputResponse,
  GetBigInput,
  GetNumbers,
  GetNumbersResponse,
  GetWords,
  GetWordsResponse,
  WsRequest,
  WsResponse
}
import esw.http.core.commons.ToMessage.ValueToMessage
import esw.http.core.commons.ToMessage.SourceToMessage
import io.bullet.borer.derivation.MapBasedCodecs._
import io.bullet.borer.{Codec, Json}

import scala.concurrent.duration.DurationLong
import scala.util.Random

object Protocol {
  sealed trait WsRequest
  case class GetNumbers(divisibleBy: Int) extends WsRequest
  case class GetWords(size: Int)          extends WsRequest
  case class GetBigInput(data: List[Int]) extends WsRequest

  sealed trait WsResponse
  case class GetNumbersResponse(number: Int)   extends WsResponse
  case class GetWordsResponse(word: String)    extends WsResponse
  case class BitInputResponse(data: List[Int]) extends WsResponse

  implicit def wsRequestCodec[T <: WsRequest]: Codec[T] = {
    implicit lazy val getNumbersCodec: Codec[GetNumbers]   = deriveCodec[GetNumbers]
    implicit lazy val getWordsCodec: Codec[GetWords]       = deriveCodec[GetWords]
    implicit lazy val getBitInputCodec: Codec[GetBigInput] = deriveCodec[GetBigInput]
    deriveCodec[WsRequest].asInstanceOf[Codec[T]]
  }

  implicit def wsRequestResponseCodec[T <: WsResponse]: Codec[T] = {
    implicit lazy val getNumbersResponseCodec: Codec[GetNumbersResponse] = deriveCodec[GetNumbersResponse]
    implicit lazy val getWordsResponseCodec: Codec[GetWordsResponse]     = deriveCodec[GetWordsResponse]
    implicit lazy val BitInputResponseCodec: Codec[BitInputResponse]     = deriveCodec[BitInputResponse]
    deriveCodec[WsResponse].asInstanceOf[Codec[T]]
  }
}

class SimpleApi {
  def getNumbers(divisibleBy: Int): Source[Int, NotUsed] = {
    Source
      .fromIterator(() => Iterator.from(1).filter(_ % divisibleBy == 0))
      .delay(1.seconds, DelayOverflowStrategy.backpressure)
  }

  def getWords(size: Int): Source[String, NotUsed] = {
    Source
      .tick(1.second, 1.second, ())
      .map(_ => Random.alphanumeric.take(size).mkString)
      .mapMaterializedValue(_ => NotUsed)
  }
}

class Handler(simpleApi: SimpleApi) {
  def handle(text: String): TextMessage = {
    Json.decode(text.getBytes()).to[WsRequest].value match {
      case GetNumbers(divisibleBy) => simpleApi.getNumbers(divisibleBy).map(GetNumbersResponse).textMessage
      case GetWords(size)          => simpleApi.getWords(size).map(GetWordsResponse).textMessage
      case GetBigInput(data)       => BitInputResponse(data).textMessage
    }
  }
}

class WsFlow(handler: Handler)(implicit mat: Materializer) {
  val value: Flow[Message, Message, NotUsed] = {
    Flow[Message].mapConcat {
      case TextMessage.Strict(text) =>
        List(handler.handle(text))
      case message: TextMessage.Streamed =>
        message.textStream.runWith(Sink.ignore)
        List.empty
      case message: BinaryMessage =>
        message.dataStream.runWith(Sink.ignore)
        List.empty
    }
  }
}
