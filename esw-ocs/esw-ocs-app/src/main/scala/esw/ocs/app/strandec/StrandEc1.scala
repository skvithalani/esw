package esw.ocs.app.strandec

import java.util.concurrent.Executor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SpawnProtocol}
import akka.util.Timeout

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContextExecutor}

class StrandEc1(val ec: Executor)(implicit val actorSystem: ActorSystem[_]) {
  def shutdown(): Unit = actorSystem.terminate()
}

object StrandEc1 {
  implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "")

  def actorBased()(implicit actorSystem: ActorSystem[_]): Executor = {
    implicit val timeout: Timeout = 5.seconds
    val actorRef                  = Await.result(actorSystem.systemActorOf(ExecutorActor.behavior, ""), 5.seconds)
    fromActor(actorRef)
  }

  private def fromActor(actorRef: ActorRef[Runnable]): Executor = new ExecutionContextExecutor {
    override def execute(runnable: Runnable): Unit     = actorRef ! runnable
    override def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
  }

  def apply(): StrandEc1 = new StrandEc1(actorBased())

}

object ExecutorActor {
  def behavior: Behavior[Runnable] = Behaviors.receiveMessage { runnable =>
    runnable.run()
    Behaviors.same
  }
}
