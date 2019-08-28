package esw.ocs.internal

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.CommandResponseManagerMessage
import csw.command.client.{CRMCacheProperties, CommandResponseManager, CommandResponseManagerActor}
import csw.framework.internal.wiring.FrameworkWiring
import csw.location.client.ActorSystemFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.highlevel.dsl.{EventServiceDsl, LocationServiceDsl, TimeServiceDsl}
import esw.ocs.syntax.FutureSyntax.FutureOps

// $COVERAGE-OFF$
private[internal] class CswServicesWiring(componentName: String) {
  lazy val actorSystem: ActorSystem[SpawnProtocol] = ActorSystemFactory.remote(SpawnProtocol.behavior, "sequencer-system")
  lazy val frameworkWiring: FrameworkWiring        = FrameworkWiring.make(actorSystem)
  implicit lazy val timeout: Timeout               = Timeouts.DefaultTimeout

  import frameworkWiring._
  import frameworkWiring.actorRuntime._

  lazy val loggerFactory      = new LoggerFactory(componentName)
  lazy val log: Logger        = loggerFactory.getLogger
  lazy val locationServiceDsl = new LocationServiceDsl(locationService)
  lazy val eventServiceDsl    = new EventServiceDsl(eventServiceFactory.make(locationService))
  lazy val timeServiceDsl     = new TimeServiceDsl(new TimeServiceSchedulerFactory)(actorSystem.scheduler)

  lazy val crmRef: ActorRef[CommandResponseManagerMessage] =
    (actorSystem ? Spawn(CommandResponseManagerActor.behavior(CRMCacheProperties(), loggerFactory), "crm")).block
  lazy val commandResponseManager: CommandResponseManager =
    new CommandResponseManager(crmRef)(actorSystem)

  lazy val sequencerCommandService: SequencerCommandServiceUtils = new SequencerCommandServiceUtils()(actorSystem)
}

// $COVERAGE-ON$
