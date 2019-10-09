package esw.gateway.server

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import csw.location.models.ComponentId
import csw.location.models.ComponentType.Assembly
import csw.params.commands.CommandResponse.{Accepted, Completed}
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.{Id, ObsId, Prefix}
import csw.params.core.states.{CurrentState, StateName}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.gateway.api.clients.CommandClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import esw.http.core.FutureEitherExt
import mscoket.impl.post.HttpPostTransport
import mscoket.impl.ws.WebsocketTransport
import msocket.api.Transport
import org.scalatest.WordSpecLike

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class CommandGatewayTest extends ScalaTestFrameworkTestKit with WordSpecLike with FutureEitherExt with GatewayCodecs {

  import frameworkTestKit._

  private val port: Int                    = 6490
  private val gatewayWiring: GatewayWiring = new GatewayWiring(Some(port))

  implicit val timeout: FiniteDuration                 = 10.seconds
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout)

  override def beforeAll(): Unit = {
    super.beforeAll()
    gatewayWiring.httpService.registeredLazyBinding.futureValue
    frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))
  }

  override protected def afterAll(): Unit = {
    gatewayWiring.httpService.shutdown(UnknownReason).futureValue
    super.afterAll()
  }

  "CommandApi" must {

    "handle validate, oneway, submit, subscribe current state and queryFinal commands | ESW-223, ESW-100, ESW-91, ESW-216, ESW-86" in {
      val postClient: Transport[PostRequest] = new HttpPostTransport[PostRequest](s"http://localhost:$port/post-endpoint", None)
      val websocketClient: Transport[WebsocketRequest] =
        new WebsocketTransport[WebsocketRequest](s"ws://localhost:$port/websocket-endpoint")
      val commandClient = new CommandClient(postClient, websocketClient)

      val componentName = "test"
      val runId         = Id("123")
      val componentType = Assembly
      val command       = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId   = ComponentId(componentName, componentType)
      val stateNames    = Set(StateName("stateName1"), StateName("stateName2"))
      val currentState1 = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
      val currentState2 = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))

      val currentStatesF: Future[Seq[CurrentState]] =
        commandClient.subscribeCurrentState(componentId, stateNames, None).take(2).runWith(Sink.seq)
      Thread.sleep(1000)

      //validate
      commandClient.validate(componentId, command).rightValue should ===(Accepted(runId))
      //oneway
      commandClient.oneway(componentId, command).rightValue should ===(Accepted(runId))
      //submit
      commandClient.submit(componentId, command).rightValue should ===(Completed(runId))

      //subscribe current state returns set of states successfully
      currentStatesF.futureValue.toSet should ===(Set(currentState1, currentState2))

      //queryFinal
      commandClient.queryFinal(componentId, runId).rightValue should ===(Completed(runId))
    }

    "handle large websocket requests" in {
      val postClient: Transport[PostRequest] = new HttpPostTransport[PostRequest](s"http://localhost:$port/post-endpoint", None)
      val websocketClient: Transport[WebsocketRequest] =
        new WebsocketTransport[WebsocketRequest](s"ws://localhost:$port/websocket-endpoint")
      val commandClient = new CommandClient(postClient, websocketClient)

      val componentName = "test"
      val runId         = Id("123")
      val componentType = Assembly
      val command       = Setup(Prefix("esw.test"), CommandName("c1"), Some(ObsId("obsId"))).copy(runId = runId)
      val componentId   = ComponentId(componentName, componentType)
      val stateNames    = (1 to 10000).toSet[Int].map(x => StateName(s"stateName$x"))
      val currentState1 = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
      val currentState2 = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))

      val currentStatesF: Future[Seq[CurrentState]] =
        commandClient.subscribeCurrentState(componentId, stateNames, None).take(2).runWith(Sink.seq)
      Thread.sleep(500)

      //oneway
      commandClient.oneway(componentId, command).futureValue.rightValue should ===(Accepted(runId))

      //subscribe current state returns set of states successfully
      currentStatesF.futureValue.toSet should ===(Set(currentState1, currentState2))
    }
  }

}
