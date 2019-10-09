package esw.ocs.impl.core

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.SequencerCommandServiceFactory
import csw.location.models.AkkaLocation
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.protocol.LoadScriptError
import esw.ocs.app.wiring.SequencerWiring
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime

class SequencerCommandServiceTest extends ScalaTestFrameworkTestKit with BaseTestSuite {
  import frameworkTestKit._
  private implicit val sys: ActorSystem[SpawnProtocol] = actorSystem

  private var wiring: SequencerWiring                                  = _
  private var sequencerLocation: Either[LoadScriptError, AkkaLocation] = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    wiring = new SequencerWiring("esw", "darknight", None)
    sequencerLocation = wiring.sequencerServer.start()
  }

  override protected def afterAll(): Unit = {
    wiring.sequencerServer.shutDown().futureValue
    super.afterAll()
  }

  "should submit and process sequence | ESW-190, ESW-148" in {
    val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
    val sequence = Sequence(command1)

    implicit val patienceConfig: PatienceConfig          = PatienceConfig(500.millis)
    val sequencerCommandService: SequencerCommandService = SequencerCommandServiceFactory.make(sequencerLocation.rightValue)
    sequencerCommandService.submitAndWait(sequence).futureValue should ===(Completed(sequence.runId))
  }
}
