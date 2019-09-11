package esw.ocs.macros

import akka.actor
import akka.actor.Scheduler
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
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

  def actorBased()(implicit actorSystem: ActorSystem[SpawnProtocol]): ExecutionContextExecutor = {
    implicit val timeout: Timeout     = 5.seconds
    implicit val scheduler: Scheduler = actorSystem.scheduler
    val actorRef: ActorRef[Runnable]  = Await.result(actorSystem ? Spawn(ExecutorActor.behavior, "strand-ec-actor"), 5.second)
    println(actorRef)
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
