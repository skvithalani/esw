package esw.ocs.core

import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.{LoadAndStartSequence, SequencerMsg}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.BaseTestSuite
import esw.ocs.api.models.Step
import esw.ocs.api.models.StepStatus.Finished
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages.SequencerResponses.{EditorResponse, LoadSequenceResponse}
import esw.ocs.internal.SequencerWiring
import org.scalatest.concurrent.PatienceConfiguration.Interval

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequencerIntegrationTest extends ScalaTestFrameworkTestKit with BaseTestSuite {

  import frameworkTestKit._
  private implicit val sys: ActorSystem[SpawnProtocol] = actorSystem

  private implicit val askTimeout: Timeout  = Timeout(5.seconds)
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  private val sequencerId   = "testSequencerId1"
  private val observingMode = "testObservingMode1"

  private var locationService: LocationService  = _
  private var wiring: SequencerWiring           = _
  private var sequencer: ActorRef[SequencerMsg] = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    locationService = HttpLocationServiceFactory.makeLocalClient
  }

  override protected def beforeEach(): Unit = {
    wiring = new SequencerWiring("testSequencerId1", "testObservingMode1", None)
    wiring.start()
    sequencer = resolveSequencer()
  }

  override protected def afterEach(): Unit = {
    wiring.shutDown().futureValue
  }

  "Sequencer" must {
    "load a sequence and start the sequence later | ESW-154" in {
      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val sequence = Sequence(command3)

      val loadResponse: Future[LoadSequenceResponse] = sequencer ? (LoadSequence(sequence, _))
      loadResponse.futureValue.response.rightValue should ===(Done)

      val startSeqResponse: Future[SubmitResponse] = sequencer ? StartSequence
      startSeqResponse.futureValue(Interval(5.seconds)) should ===(Completed(sequence.runId))
    }

    "process sequence and execute commands that are added later | ESW-145, ESW-154" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Setup(Prefix("test"), CommandName("command-2"), None)
      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val sequence = Sequence(command1, command2)

      val processSeqResponse: Future[SubmitResponse] = sequencer ? (LoadAndStartSequence(sequence, _))

      val addResponse: Future[EditorResponse] = sequencer ? (Add(List(command3), _))
      addResponse.futureValue.response.rightValue should ===(Done)

      processSeqResponse.futureValue should ===(Completed(sequence.runId))

      (sequencer ? GetSequence).futureValue.steps should ===(
        List(
          Step(command1, Finished.Success(Completed(command1.runId)), hasBreakpoint = false),
          Step(command2, Finished.Success(Completed(command2.runId)), hasBreakpoint = false),
          Step(command3, Finished.Success(Completed(command3.runId)), hasBreakpoint = false)
        )
      )
    }
  }

  private def resolveSequencer(): ActorRef[SequencerMsg] =
    locationService
      .resolve(AkkaConnection(ComponentId(s"$sequencerId@$observingMode", ComponentType.Sequencer)), 5.seconds)
      .futureValue
      .value
      .uri
      .toActorRef
      .unsafeUpcast[SequencerMsg]
}
