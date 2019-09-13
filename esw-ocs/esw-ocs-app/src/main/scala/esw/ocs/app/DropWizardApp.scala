package esw.ocs.app

import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.client.internal.SequencerCommandServiceImpl
import csw.location.models.AkkaLocation
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import esw.ocs.api.protocol.RegistrationError
import esw.ocs.app.wiring.SequencerWiring
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps

object DropWizardApp extends App {
  val wiring: SequencerWiring                               = new SequencerWiring("testSequencerId", "testObservingMode", None)
  val registration: Either[RegistrationError, AkkaLocation] = wiring.sequencerServer.start()

  implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "poorva-test-app")

  registration.map { location =>
    val commandService = new SequencerCommandServiceImpl(location)
    val setup          = Setup(Prefix("wfos.home.datum"), CommandName("iris"), None)
    val sequence       = Sequence(setup)
    commandService.submitAndWait(sequence)
  }

  CoordinatedShutdown(actorSystem.toUntyped)
}
