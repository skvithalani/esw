package esw.ocs.app.metrics

import java.util.concurrent.{CountDownLatch, ThreadPoolExecutor}

import esw.ocs.dsl.{CswServices, Script}

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class TestScript(csw: CswServices) extends Script(csw) {
  override val loopInterval: FiniteDuration = 100.millis
  val latch                                 = new CountDownLatch(3)

  loop(1.minute) {
    spawn {
      ThreadPoolExecutor.Thread.sleep(1000 * 60)
      println("thread loop")
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
