package esw.ocs.impl.dsl

import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.EventService
import csw.location.api.scaladsl.LocationService
import csw.time.core.models.UTCTime
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.highlevel.dsl.{EventServiceDsl, LocationServiceDsl, TimeServiceDsl}
import esw.ocs.impl.core.SequenceOperator
import esw.ocs.impl.internal.SequencerCommandServiceDsl

import scala.collection.convert.ImplicitConversionsToScala.`set asScala`
import scala.concurrent.{ExecutionContext, Future}

class CswServices(
    private[ocs] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    private[esw] val actorSystem: ActorSystem[_],
    private[esw] val locationService: LocationService,
    private[esw] val eventService: EventService,
    private[esw] val timeServiceSchedulerFactory: TimeServiceSchedulerFactory
) extends SequencerCommandServiceDsl
    with LocationServiceDsl
    with EventServiceDsl
    with TimeServiceDsl {

  def getNumber: Future[Unit] =
    Future {
      println("sleeping")
      getThread("pool-10-thread-1").foreach { x =>
        println(s"[1000] - Before sleep ${UTCTime.now()} ${x.getName} --> ${x.getState} ")
      }
      Thread.sleep(5000)
      getThread("pool-10-thread-1").foreach { x =>
        println(s"[1000] - After sleep ${UTCTime.now()} ${x.getName} --> ${x.getState} ")
      }
    }(ExecutionContext.global)

  def getThread(name: String): Option[Thread] = {
    if (name == null) throw new NullPointerException("Null name")
    val threads: List[Thread] = Thread.getAllStackTraces.keySet().toList

    threads.find(_.getName == name)
  }

}
