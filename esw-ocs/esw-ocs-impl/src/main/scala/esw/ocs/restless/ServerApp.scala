package esw.ocs.restless

import esw.ocs.internal.SequencerWiring

object ServerApp extends App {
  private val wiring = new SequencerWiring("ocs", "darknight")

  wiring.server.startServer("0.0.0.0", 6000)
}

object ClientApp extends App {
  private val wiring = new SequencerWiring("ocs", "darknight")
  import wiring.actorRuntime._

  wiring.client.status.foreach(println)
}
