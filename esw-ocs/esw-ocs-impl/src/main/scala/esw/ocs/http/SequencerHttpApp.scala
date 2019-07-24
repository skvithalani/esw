package esw.ocs.http

import akka.actor
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import esw.ocs.internal.SequencerWiring

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object SequencerHttpApp {
  def main(args: Array[String]) {

    implicit val typedSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")
    implicit val system: actor.ActorSystem               = typedSystem.toUntyped

    implicit val materializer: Materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContext = typedSystem.executionContext

    val wiring = new SequencerWiring("iris", "darknight")
    import wiring._
    val route = new Route(wiring.sequencer).route
    engine.start(sequenceOperatorFactory(), script)

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind())                      // trigger unbinding from the port
      .onComplete(_ => typedSystem.terminate()) // and shutdown when done
  }
}
