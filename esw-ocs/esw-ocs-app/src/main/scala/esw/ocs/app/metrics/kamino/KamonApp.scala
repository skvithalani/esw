package esw.ocs.app.metrics.kamino

import java.util

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.client.internal.SequencerCommandServiceImpl
import csw.location.models.AkkaLocation
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol.RegistrationError
import esw.ocs.app.SequencerApp.sequencerWiringWithHttp
import esw.ocs.app.wiring.SequencerWiring
import kamon.Kamon

import scala.collection.convert.ImplicitConversionsToScala.`set asScala`

object KamonApp extends App {

  println("*******Kamon init started******")
  Kamon.init()
  println("*******Kamon initialized******")
  val wiring: SequencerWiring                               = sequencerWiringWithHttp("testSequencerId", "testObservingMode", None)
  val registration: Either[RegistrationError, AkkaLocation] = wiring.sequencerServer.start()

  implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "poorva-test-app")

  registration.map { location =>
    val commandService = new SequencerCommandServiceImpl(location)
    val setup          = Setup(Prefix("wfos.home.datum"), CommandName("iris"), None)
    val sequence       = Sequence(setup)
    commandService.submit(sequence)
  }

  Thread.sleep(3000)

  def getThread(name: String): Option[Thread] = {
    if (name == null) throw new NullPointerException("Null name")
    val threads: List[Thread] = Thread.getAllStackTraces.keySet().toList

    threads.find(_.getName == name)
  }

  getThread("pool-10-thread-1").map { x =>
    println(s"[1000] - ${UTCTime.now()} ${x.getName} --> ${x.isInterrupted} ")
  }

}
