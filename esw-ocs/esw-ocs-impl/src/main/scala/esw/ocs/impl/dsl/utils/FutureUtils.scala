package esw.ocs.impl.dsl.utils

import akka.actor.Scheduler
import csw.time.core.models.UTCTime
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.macros.StrandEc

import scala.async.Async.{async, await}
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Future, Promise}

private[ocs] object FutureUtils {

  /**
   * returns a future which completes either
   * after minDelay or function Completion; whichever takes longer time
   *
   */
  def delayedResult[T](
      minDelay: FiniteDuration
  )(f: => Future[T])(implicit strandEc: StrandEc): Future[T] = {
    async {
      val delayFuture = delay(minDelay, strandEc)
      val futureValue = f
      await(delayFuture)
      await(futureValue)
    }(strandEc.executor)
  }

  private def delay(duration: FiniteDuration, strandEc: StrandEc): Future[Unit] = {
    println("Delaying")

    implicit val scheduler: Scheduler = strandEc.untypedActorSystem.scheduler

    val p    = Promise[Unit]()
    val time = UTCTime.after(duration)

    println(s"Current at ${UTCTime.now()}")
    println(s"Scheduling at $time")

    new TimeServiceSchedulerFactory().make()(strandEc.executor).scheduleOnce(time) { () =>
      println("Delay finished")
      p.success(())
    }
    p.future
  }
}

object Sample extends App {
  private implicit val ec = StrandEc()

  FutureUtils.delayedResult(500.millis) {
    Future.successful(println("Result"))
  }

  Thread.sleep(1000)
}
