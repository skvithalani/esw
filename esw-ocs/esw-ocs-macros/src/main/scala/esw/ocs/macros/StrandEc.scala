package esw.ocs.macros

import akka.actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SpawnProtocol}
import akka.util.Timeout

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContextExecutor}

class StrandEc(val executor: ExecutionContextExecutor)(implicit val _actorSystem: ActorSystem[_]) {
  val untypedActorSystem: actor.ActorSystem = _actorSystem.toUntyped
  def shutdown(): Unit                      = _actorSystem.terminate()
}

object StrandEc {
  implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "strand-ec-actor-system")

  def actorBased()(implicit actorSystem: ActorSystem[_]): ExecutionContextExecutor = {
    implicit val timeout: Timeout = 5.seconds
    val actorRef                  = Await.result(actorSystem.systemActorOf(ExecutorActor.behavior, "strand-ec-actor"), 5.seconds)
    fromActor(actorRef)
  }

  private def fromActor(actorRef: ActorRef[Runnable]): ExecutionContextExecutor = new ExecutionContextExecutor {
    override def execute(runnable: Runnable): Unit     = actorRef ! runnable
    override def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
  }

  def apply(): StrandEc = new StrandEc(actorBased())

}

object ExecutorActor {
  def behavior: Behavior[Runnable] = Behaviors.receiveMessage { runnable =>
    runnable.run()
    Behaviors.same
  }
}
