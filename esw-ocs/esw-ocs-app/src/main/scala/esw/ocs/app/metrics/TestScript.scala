package esw.ocs.app.metrics

import java.util.concurrent.CountDownLatch

import esw.ocs.impl.dsl.{CswServices, Script}

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class TestScript(csw: CswServices) extends Script(csw) {
  override val loopInterval: FiniteDuration = 100.millis
  val latch                                 = new CountDownLatch(3)

  loop(1.second) {
    spawn {
      println("loop")
      stopIf(false)
    }
  }
  handleSetupCommand("iris") { _ =>
    spawn {

      // await utility provided in ControlDsl, asynchronously blocks for future to complete
      println("done")
    }
  }

}
