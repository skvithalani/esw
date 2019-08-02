package esw.ocs.core

import esw.ocs.internal.SequenceComponentWiring

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object ReloadTestApp extends App {
  private val sequenceComponentWiring = new SequenceComponentWiring(1)
  import sequenceComponentWiring.actorRuntime._
  sequenceComponentWiring.start()

  private val sequenceComponentClient = new SequenceComponentClient(sequenceComponentWiring.sequenceComponentRef)

  val begin: Long = System.currentTimeMillis()
  Await.result(sequenceComponentClient.loadScript("iris", "darknight"), 10.seconds)
  val intermediate: Long = System.currentTimeMillis()
  Await.result(sequenceComponentClient.unloadScript(), 10.seconds)
  val intermediate1: Long = System.currentTimeMillis()
  Await.result(sequenceComponentClient.loadScript("iris", "darknight"), 10.seconds)
  val end: Long = System.currentTimeMillis()

  println(s"First load time ${intermediate - begin} millis")
  println(s"Unload time ${intermediate1 - intermediate} millis")
  println(s"Reload time ${end - intermediate} millis")
}
