package esw.ocs.app.metrics

import java.util.concurrent.{CountDownLatch, Executor, Executors}

import esw.ocs.impl.dsl.{CswServices, Script}
import sun.jvm.hotspot.runtime.Threads

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class TestScript(csw: CswServices) extends Script(csw) {
  override val loopInterval: FiniteDuration = 100.millis
  val latch                                 = new CountDownLatch(3)
  var counter                               = 0

  loop(5.second) {
    spawn {
      counter += 1
      println(s"loop $counter")
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
