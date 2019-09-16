package esw.ocs.macros

import java.util.concurrent.{Executors, ScheduledExecutorService}

import com.codahale.metrics.{InstrumentedScheduledExecutorService, MetricRegistry}

import scala.concurrent.ExecutionContext

class StrandEc private (private[ocs] val executorService: ScheduledExecutorService, val registry: MetricRegistry) {
  val ec: ExecutionContext = ExecutionContext.fromExecutorService(executorService)
  def shutdown(): Unit     = executorService.shutdownNow()
}

object StrandEc {
  def apply(): StrandEc = {
    val registry = new MetricRegistry()

    val service                  = Executors.newSingleThreadScheduledExecutor()
    val scheduledExecutorService = new InstrumentedScheduledExecutorService(service, registry, "test-strand-ec")

    new StrandEc(scheduledExecutorService, registry)
  }
}
