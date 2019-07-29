package esw.ocs.internal

import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.CommandResponseManagerMessage
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.{CRMCacheProperties, CommandResponseManager, CommandResponseManagerActor}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import esw.ocs.api.models.messages.error.RegistrationError
import esw.ocs.core._
import esw.ocs.dsl.utils.ScriptLoader
import esw.ocs.dsl.{CswServices, Script}
import esw.ocs.macros.StrandEc
import esw.ocs.restless.{SeqMsgClient, SeqMsgServer, SequencerImplStub}
import esw.ocs.syntax.FutureSyntax.FutureOps
import esw.ocs.utils.RegistrationUtils
// $COVERAGE-OFF$
private[ocs] class SequencerWiring(val sequencerId: String, val observingMode: String) {
  private lazy val config          = ConfigFactory.load()
  private lazy val sequencerConfig = SequencerConfig.from(config, sequencerId, observingMode)
  import sequencerConfig._
  lazy val name: String = sequencerName
  lazy val actorRuntime = new ActorRuntime(sequencerName)
  import actorRuntime._

  private lazy val engine      = new Engine()
  private lazy val componentId = ComponentId(sequencerName, ComponentType.Sequencer)

  private lazy val crmRef: ActorRef[CommandResponseManagerMessage] =
    (typedSystem ? Spawn(CommandResponseManagerActor.behavior(CRMCacheProperties(), loggerFactory), "crm")).block
  private lazy val commandResponseManager: CommandResponseManager = new CommandResponseManager(crmRef)

  lazy val sequencerRef: ActorRef[SequencerMsg] =
    (typedSystem ? Spawn(SequencerBehavior.behavior(sequencer, script), sequencerName)).block

  //Pass lambda to break circular dependency shown below.
  //SequencerRef -> Script -> cswServices -> SequencerOperator -> SequencerRef
  private lazy val sequenceOperatorFactory = () => new SequenceOperator(sequencerRef)

  private lazy val cswServices    = new CswServices(sequenceOperatorFactory, commandResponseManager)
  private lazy val script: Script = ScriptLoader.load(scriptClass, cswServices)
  lazy val strandEc               = StrandEc()
  lazy val sequencer              = new Sequencer(commandResponseManager)(strandEc, timeout)

  lazy val sequenceEditorClient = new SequenceEditorClient(sequencerRef)

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  // fixme: no need to block
  def shutDown(): Unit = {
    locationService.unregister(AkkaConnection(componentId)).block
    sequenceEditorClient.shutdown().block
    strandEc.shutdown()
    typedSystem.terminate()
  }

  def start(): Either[RegistrationError, AkkaLocation] = {
    engine.start(sequenceOperatorFactory(), script)

    val registration = AkkaRegistration(AkkaConnection(componentId), prefix, sequencerRef.toURI)
    RegistrationUtils.register(locationService, registration)(coordinatedShutdown).block
  }

  lazy val implStub = new SequencerImplStub()
  lazy val server   = new SeqMsgServer(implStub, null)
  lazy val client   = new SeqMsgClient(actorRuntime.typedSystem)

}
// $COVERAGE-ON$
