package esw.http.core.commons

import akka.actor.CoordinatedShutdown

object CoordinatedShutdownReasons {
  case object ApplicationFinishedReason   extends CoordinatedShutdown.Reason
  case class FailureReason(ex: Throwable) extends CoordinatedShutdown.Reason
}
