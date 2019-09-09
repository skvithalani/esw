package esw.ocs.macros

import java.util.concurrent.{Executors, ScheduledExecutorService}

import scala.concurrent.ExecutionContext

class StrandEc private (private[ocs] val executorService: ScheduledExecutorService) {
  val ec: ExecutionContext = ExecutionContext.fromExecutorService(executorService)
  def shutdown(): Unit     = executorService.shutdownNow()
}

object StrandEc {
  def apply(): StrandEc = new StrandEc(Executors.newSingleThreadScheduledExecutor())
}

//todo: Add csw logging step in report failure so that if executorService fails it will error log in the system
