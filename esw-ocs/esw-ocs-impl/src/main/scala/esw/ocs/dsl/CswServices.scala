package esw.ocs.dsl

import java.util
import java.util.concurrent.CompletionStage
import java.util.function.Consumer

import akka.Done
import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.{EventService, EventSubscription}
import csw.location.api.scaladsl.LocationService
import csw.params.events.Event
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.highlevel.dsl.{EventServiceDsl, LocationServiceDsl, TimeServiceDsl}
import esw.ocs.core.SequenceOperator
import esw.ocs.internal.SequencerCommandServiceDsl

import scala.compat.java8.FutureConverters.FutureOps
import scala.jdk.CollectionConverters.{SetHasAsJava, _}
import scala.jdk.FunctionConverters.enrichAsScalaFromConsumer

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

  import actorSystem.executionContext

  // ============================
  def jGetEvent(eventKeys: util.Set[java.lang.String]): CompletionStage[util.Set[Event]] =
    getEvent(eventKeys.asScala.toSeq: _*).map(_.asJava).toJava
  def jPublishEvent(event: Event): CompletionStage[Done] = publishEvent(event).toJava
  def jOnEvent(eventKeys: util.Set[java.lang.String])(callback: Consumer[Event]): EventSubscription =
    onEvent0(eventKeys.asScala.toSeq: _*)(callback.asScala)
}
//    sequenceId: String,
//    observingMode: String
