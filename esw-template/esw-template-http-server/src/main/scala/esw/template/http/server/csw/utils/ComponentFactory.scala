package esw.template.http.server.csw.utils

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.command.client.ICommandServiceFactory
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

class ComponentFactory(locationService: LocationService, commandServiceFactory: ICommandServiceFactory)(
    implicit typedSystem: ActorSystem[_],
    ec: ExecutionContext
) {

  private[server] def resolve[T](componentName: String, componentType: ComponentType)(f: AkkaLocation => T): Future[T] =
    locationService
      .resolve(AkkaConnection(ComponentId(componentName, componentType)), 5.seconds)
      .map {
        case Some(akkaLocation) => f(akkaLocation)
        case None               => throw new IllegalArgumentException(s"Could not find component - $componentName of type - $componentType")
      }

  def commandService(componentName: String, componentType: ComponentType): Future[CommandService] =
    resolve(componentName, componentType)(commandServiceFactory.make)
}
