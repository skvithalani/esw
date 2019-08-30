package esw.ocs.dsl

import java.util.concurrent.{CompletableFuture, CompletionStage}

import akka.Done
import csw.params.commands.{Observe, SequenceCommand, Setup}
import esw.ocs.api.models.responses.PullNextResult
import esw.ocs.dsl.utils.{FunctionBuilder, FunctionHandlers}
import esw.ocs.exceptions.UnhandledCommandException

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble
import scala.jdk.FutureConverters.FutureOps
import scala.reflect.ClassTag

class Script(val csw: CswServices) extends ScriptDsl {
  // todo: should this come from conf file?
  override val loopInterval = 50.millis
}

trait ScriptDsl extends ControlDsl {
  def csw: CswServices

  var isOnline = true

  val commandHandlerBuilder: FunctionBuilder[SequenceCommand, Future[Unit]] = new FunctionBuilder

  val onlineHandlers: FunctionHandlers[Unit, Future[Unit]]   = new FunctionHandlers
  val offlineHandlers: FunctionHandlers[Unit, Future[Unit]]  = new FunctionHandlers
  val shutdownHandlers: FunctionHandlers[Unit, Future[Unit]] = new FunctionHandlers
  val abortHandlers: FunctionHandlers[Unit, Future[Unit]]    = new FunctionHandlers

  def handle[T <: SequenceCommand: ClassTag](name: String)(handler: T => Future[Unit]): Unit =
    commandHandlerBuilder.addHandler[T](handler)(_.commandName.name == name)

  lazy val commandHandler: SequenceCommand => Future[Unit] =
    commandHandlerBuilder.build { input =>
      // should script writer have ability to add this default handler, like handleUnknownCommand
      spawn {
        throw new UnhandledCommandException(input)
      }
    }

  def execute(command: SequenceCommand): Future[Unit]           = spawn(commandHandler(command).await)
  def jExecute(command: SequenceCommand): CompletionStage[Void] = spawn(commandHandler(command).await).asJava.thenAccept(_ => ())

  def executeGoOnline(): Future[Done] =
    Future.sequence(onlineHandlers.execute(())).map { _ =>
      isOnline = true
      Done
    }

  def executeGoOffline(): Future[Done] = {
    isOnline = false
    Future.sequence(offlineHandlers.execute(())).map(_ => Done)
  }

  def executeShutdown(): Future[Done] = Future.sequence(shutdownHandlers.execute(())).map(_ => Done)

  def executeAbort(): Future[Done] = Future.sequence(abortHandlers.execute(())).map(_ => Done)

  def nextIf(f: SequenceCommand => Boolean): Future[Option[SequenceCommand]] =
    spawn {
      val operator  = csw.sequenceOperatorFactory()
      val mayBeNext = operator.maybeNext.await
      mayBeNext match {
        case Some(step) if f(step.command) =>
          operator.pullNext.await match {
            case PullNextResult(step) => Some(step.command)
            case _                    => None
          }
        case _ => None
      }
    }

  def handleSetupCommand(name: String)(handler: Setup => Future[Unit]): Unit = handle(name)(handler)
  def jHandleSetupCommand(name: String)(handler: Setup => CompletableFuture[Void]): Void = {
    handle(name)((command: Setup) => handler(command).toScala.map(_ => ()))
    null
  }

  def handleObserveCommand(name: String)(handler: Observe => Future[Unit]): Unit = handle(name)(handler)
//  def jHandleObserveCommand(name: String)(handler: Observe => CompletableFuture[Unit]): Unit =
//    handle(name)((command: Observe) => handler(command).toScala)

  def handleGoOnline(handler: => Future[Unit]): Unit  = onlineHandlers.add(_ => handler)
  def handleGoOffline(handler: => Future[Unit]): Unit = offlineHandlers.add(_ => handler)
  def handleShutdown(handler: => Future[Unit]): Unit  = shutdownHandlers.add(_ => handler)
  def handleAbort(handler: => Future[Unit]): Unit     = abortHandlers.add(_ => handler)
}
