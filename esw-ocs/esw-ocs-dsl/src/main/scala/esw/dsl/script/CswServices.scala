package esw.dsl.script

import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.event.api.javadsl.IEventService
import csw.location.api.javadsl.ILocationService
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.dsl.script.utils.LockUnlockUtil
import esw.ocs.api.SequencerAdminFactoryApi

class CswServices(
    private[esw] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    val actorSystem: ActorSystem[_],
    private[esw] val locationService: ILocationService,
    private[esw] val eventService: IEventService,
    private[esw] val timeServiceSchedulerFactory: TimeServiceSchedulerFactory,
    private[esw] val sequencerAdminFactory: SequencerAdminFactoryApi,
    private[esw] val lockUnlockUtil: LockUnlockUtil
)
