package esw.gateway.impl

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

import akka.Done
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.logging.models.Level
import esw.gateway.api.LoggingApi
import io.bullet.borer.Dom.{MapElem, _}

import scala.concurrent.{ExecutionContext, Future}

class LoggingImpl(loggerCache: LoggerCache)(implicit ec: ExecutionContext) extends LoggingApi {

  def getValue(element: Element): Any = {
    element match {
      case NullElem                => null
      case BooleanElem(value)      => value
      case StringElem(value)       => value
      case IntElem(value)          => value
      case LongElem(value)         => value
      case NumberStringElem(value) => value
      case DoubleElem(value)       => value
      case UndefinedElem           => null
      case FloatElem(value)        => value
      case Float16Elem(value)      => value
      case elem: ArrayElem         => Array(elem.elements.map(getValue))
      case elem: MapElem           => getMap(elem)
      case _                       => null
    }
  }

  def getMap(element: MapElem): Map[String, Any] = element.asInstanceOf[MapElem].toMap.map[String, Any] {
    case (k, v) => (k.asInstanceOf[StringElem].value, getValue(v))
  }

  override def log(appName: String, level: Level, message: String, metadata: MapElem): Future[Done] = {
    val logger = loggerCache.get(appName)
    val map    = getMap(metadata)
    level match {
      case Level.TRACE => logger.trace(message, map)
      case Level.DEBUG => logger.debug(message, map)
      case Level.INFO  => logger.info(message, map)
      case Level.WARN  => logger.warn(message, map)
      case Level.ERROR => logger.error(message, map)
      case Level.FATAL => logger.fatal(message, map)
    }
    Future.successful(Done)
  }
}

class LoggerCache {

  /**
   * This value represents maximum number of frontend clients who can send log message to
   * this gateway instance
   */
  private val maxClients = 2048

  /**
   * This concurrent map represents an eventually built up cache of LoggerFactory
   * instances. Each LoggerFactory instance is for a different front-end client
   */
  private val serviceLoggers = new ConcurrentHashMap[String, Logger](maxClients)

  private def newLogger(name: String): Function[String, Logger] = _ => new LoggerFactory(name.toLowerCase).getLogger

  def get(componentName: String): Logger = serviceLoggers.computeIfAbsent(componentName.toLowerCase, newLogger(componentName))
}
