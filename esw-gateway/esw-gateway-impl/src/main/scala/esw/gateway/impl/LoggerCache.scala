package esw.gateway.impl

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory

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
