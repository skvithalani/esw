package esw.gateway.server2

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import csw.location.models.ComponentId
import csw.location.models.ComponentType.Assembly
import csw.params.commands.CommandResponse.{Accepted, Completed}
import csw.params.commands.{CommandName, ControlCommand, Setup}
import csw.params.core.models.{Id, ObsId, Prefix}
import csw.params.core.states.{CurrentState, StateName}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.gateway.api.clients.CommandClient
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.CommandAction.{Oneway, Submit, Validate}
import esw.gateway.api.messages.{PostRequest, WebsocketRequest}
import esw.http.core.FutureEitherExt
import mscoket.impl.post.PostClient
import mscoket.impl.ws.WebsocketClient
import msocket.api.RequestClient
import org.scalatest.WordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class CommandGatewayTest extends ScalaTestFrameworkTestKit with WordSpecLike with FutureEitherExt with RestlessCodecs {

  import frameworkTestKit._

  private val port: Int                    = 6490
  private val gatewayWiring: GatewayWiring = new GatewayWiring(Some(port))

  implicit val timeout: FiniteDuration                 = 10.seconds
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout)

  override def beforeAll(): Unit = {
    super.beforeAll()
    gatewayWiring.httpService.registeredLazyBinding.futureValue
  }

  override protected def afterAll(): Unit = {
    gatewayWiring.httpService.shutdown(UnknownReason).futureValue
    super.afterAll()
  }

  "CommandApi" must {

    "handle validate, oneway, submit, subscribe current state and queryFinal commands | ESW-216" in {
      val postClient: RequestClient[PostRequest] = new PostClient[PostRequest](s"http://localhost:$port/post")
      val websocketClient: RequestClient[WebsocketRequest] =
        new WebsocketClient[WebsocketRequest](s"ws://localhost:$port/websocket")
      val commandClient = new CommandClient(postClient, websocketClient)

      frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))

      val componentName           = "test"
      val runId                   = Id("123")
      val componentType           = Assembly
      val command: ControlCommand = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId             = ComponentId(componentName, componentType)
      val stateNames              = Set(StateName("stateName1"), StateName("stateName2"))
      val currentState1           = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
      val currentState2           = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))

      val currentStatesF: Future[Seq[CurrentState]] =
        commandClient.subscribeCurrentState(componentId, stateNames, None).take(2).runWith(Sink.seq)
      Thread.sleep(1000)

      //validate
      commandClient.process(componentId, command, Validate).rightValue should ===(Accepted(runId))
      //oneway
      commandClient.process(componentId, command, Oneway).rightValue should ===(Accepted(runId))
      //submit
      commandClient.process(componentId, command, Submit).rightValue should ===(Completed(runId))

      //subscribe current state returns set of states successfully
      currentStatesF.futureValue.toSet should ===(Set(currentState1, currentState2))

      //queryFinal
      commandClient.queryFinal(componentId, runId).rightValue should ===(Completed(runId))
    }
  }

}
