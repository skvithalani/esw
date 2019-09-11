package esw.ocs.macros

import java.util.concurrent.{Executors, ScheduledExecutorService}

import kamon.instrumentation.executor.ExecutorInstrumentation

import scala.concurrent.ExecutionContext

class StrandEc private (private[ocs] val executorService: ScheduledExecutorService) {
  val ec: ExecutionContext = ExecutionContext.fromExecutorService(executorService)
  def shutdown(): Unit     = executorService.shutdownNow()
}

object StrandEc {

  def apply(): StrandEc = {
    val executor = Executors.newSingleThreadScheduledExecutor()
    ExecutorInstrumentation.instrument(executor, "poorva-test-executor")
    new StrandEc(executor)
  }
}
