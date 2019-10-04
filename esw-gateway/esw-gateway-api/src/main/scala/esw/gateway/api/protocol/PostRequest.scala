package esw.gateway.api.protocol

import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.location.models.ComponentId
import csw.logging.models.Level
import csw.params.commands.ControlCommand
import csw.params.events.{Event, EventKey}
import io.bullet.borer.Dom.MapElem

sealed trait PostRequest

object PostRequest {
  case class Submit(componentId: ComponentId, command: ControlCommand)              extends PostRequest
  case class Oneway(componentId: ComponentId, command: ControlCommand)              extends PostRequest
  case class Validate(componentId: ComponentId, command: ControlCommand)            extends PostRequest
  case class PublishEvent(event: Event)                                             extends PostRequest
  case class GetEvent(eventKeys: Set[EventKey])                                     extends PostRequest
  case class SetAlarmSeverity(alarmKey: AlarmKey, severity: AlarmSeverity)          extends PostRequest
  case class Log(appName: String, level: Level, message: String, metadata: MapElem) extends PostRequest
}
