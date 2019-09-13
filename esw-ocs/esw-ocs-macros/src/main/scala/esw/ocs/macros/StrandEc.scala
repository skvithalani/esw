package esw.ocs.macros

import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import com.codahale.metrics.{ConsoleReporter, MetricRegistry}
import io.dropwizard.lifecycle.setup.LifecycleEnvironment

import scala.concurrent.ExecutionContext

class StrandEc private (private[ocs] val executorService: ScheduledExecutorService) {
  val ec: ExecutionContext = ExecutionContext.fromExecutorService(executorService)
  def shutdown(): Unit     = executorService.shutdownNow()
}

object StrandEc {
  def apply(): StrandEc = {
    val registry    = new MetricRegistry()
    val environment = new LifecycleEnvironment(registry)
    val service: ScheduledExecutorService = environment
      .scheduledExecutorService("strand-ec-executor-service")
      .threads(1)
      .build()

    val reporter =
      ConsoleReporter
        .forRegistry(registry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build
    reporter.start(1, TimeUnit.SECONDS)

    new StrandEc(service)
  }
}
