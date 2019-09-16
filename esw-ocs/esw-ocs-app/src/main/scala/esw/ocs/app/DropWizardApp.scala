package esw.ocs.app

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.codahale.metrics.jmx.JmxReporter
import com.codahale.metrics.{ConsoleReporter, MetricFilter}
import csw.command.client.internal.SequencerCommandServiceImpl
import csw.location.models.AkkaLocation
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import esw.ocs.api.protocol.RegistrationError
import esw.ocs.app.wiring.SequencerWiring
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.dropwizard.DropwizardExports
import io.prometheus.client.exporter.MetricsServlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}

object DropWizardApp extends App {
  val wiring: SequencerWiring                               = new SequencerWiring("testSequencerId", "testObservingMode", None)
  val registration: Either[RegistrationError, AkkaLocation] = wiring.sequencerServer.start()

  private val registry = wiring.script.strandEc.registry
  val consoleReporter =
    ConsoleReporter
      .forRegistry(registry)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build

  println("*****Starting Console reporter*******")
  consoleReporter.start(1, TimeUnit.SECONDS)
  println("*****Console Reporter started*******")
  implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "poorva-test-app")

  val jmxReporter: JmxReporter = JmxReporter.forRegistry(registry).build
  println("*****Starting JmxReporter reporter*******")
  jmxReporter.start()
  println("*****JmxReporter Reporter started*******")

  val graphite = new Graphite(new InetSocketAddress("localhost", 8000))
  val graphiteReporter = GraphiteReporter
    .forRegistry(registry)
    .prefixedWith("strand-ec-metrics")
    .convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .filter(MetricFilter.ALL)
    .build(graphite)

  println("*****Starting GraphiteReporter reporter*******")
  graphiteReporter.start(1, TimeUnit.SECONDS)
  println("*****GraphiteReporter Reporter started*******")

  registration.map { location =>
    val commandService = new SequencerCommandServiceImpl(location)
    val setup          = Setup(Prefix("wfos.home.datum"), CommandName("iris"), None)
    val sequence       = Sequence(setup)
    commandService.submitAndWait(sequence)
  }

  private val exports                      = new DropwizardExports(registry)
  private val registry1: CollectorRegistry = CollectorRegistry.defaultRegistry
  registry1.register(exports)

  val server  = new Server(8081)
  val context = new ServletContextHandler()
  context.setContextPath("/")
  server.setHandler(context)
  context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics")
  // Start the webserver.
  server.start()

  CoordinatedShutdown(actorSystem.toUntyped)
}
