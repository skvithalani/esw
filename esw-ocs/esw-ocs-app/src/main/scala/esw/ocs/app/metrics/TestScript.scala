package esw.ocs.app.metrics

import java.util.concurrent.CountDownLatch

import esw.ocs.impl.dsl.{CswServices, Script}

import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class TestScript(csw: CswServices) extends Script(csw) {
  override val loopInterval: FiniteDuration = 100.millis
  val latch                                 = new CountDownLatch(3)

  /*(1 to 100000000).foreach { _ =>
    //------> constructor time thread
    Future {
      //--------> strandec
      println("in future")
    }
  } //-----------> Runnable*/
  /*loop(2.second) {
    spawn {
      println("looping")
      stopIf(false)
    }
  }//---------> Timed_Waiting*/

  /*loop(2.second) {
    //----> constructor time thread
    spawn {
      println("looping")
      csw.getNumber.await //-------> await on stranec getNumber ---> global ec
      stopIf(false)
    }
  } //---------> Timed_Waiting & Waiting*/

  /*(1 to 100000000).foreach { _ =>
    spawn {
      csw.getNumber.await
      println("in future")
    }
  } //-----------> Runnable
 */
}
