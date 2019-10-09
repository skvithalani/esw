package esw.ocs.dsl.utils

import io.kotlintest.matchers.numerics.shouldBeInRange
import io.kotlintest.matchers.numerics.shouldBeLessThan
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.time.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoopTest : WordSpec({

    "loop" should {
        "run till condition becomes true when interval is default | ESW-89" {
            val counter = AtomicInteger(0)

            val loopTime = measureTimeMillis {
                loop {
                    counter.getAndUpdate { it + 1 }
                    stopWhen(counter.get() == 5)
                }
            }

            counter.get() shouldBe 5
            // default interval is 50ms, loop should run 5 times which means it should take around 50*5=250ms
            loopTime shouldBeInRange 250L..300L
        }

        "run till condition becomes true when interval is custom | ESW-89" {
            val counter = AtomicInteger(0)

            val loopTime = measureTimeMillis {
                loop(300.milliseconds) {
                    counter.getAndUpdate { it + 1 }
                    stopWhen(counter.get() == 3)
                }
            }

            counter.get() shouldBe 3
            // custom loop interval is 300ms, loop should run 3 times which means it should take around 300*3=900ms
            loopTime shouldBeInRange 900L..1000L
        }

        "allow stopWhen conditions to be specified any number of times and anywhere in the loop body | ESW-89" {
            val counter1 = AtomicInteger(0)
            val counter2 = AtomicInteger(0)

            loop {
                counter1.getAndUpdate { it + 1 }
                stopWhen(counter1.get() == 5)

                counter2.getAndUpdate { it + 2 }
                stopWhen(counter2.get() == 10)
            }

            // on 5th iteration, counter1 becomes 5 and first stopWhen matches
            // loop gets terminated there and does not execute rest of the body
            counter1.get() shouldBe 5
            counter2.get() shouldBe 8
        }
    }

    "bgLoop" should {
        "run in the background till condition becomes true when interval is default | ESW-89" {
            val counter = AtomicInteger(0)

            val loopTime = measureTimeMillis {
                val loopResult = bgLoop {
                    counter.getAndUpdate { it + 1 }
                    stopWhen(counter.get() == 5)
                }

                // here counter is less than 5 proves that loop is still running in the background
                counter.get() shouldBeLessThan 5
                loopResult.await()
            }

            counter.get() shouldBe 5
            // default interval is 50ms, loop should run 5 times which means it should take around 50*5=250ms
            loopTime shouldBeInRange 250L..300L
        }

        "run in the background till condition becomes true when interval is custom | ESW-89" {
            val counter = AtomicInteger(0)

            val loopTime = measureTimeMillis {
                val loopResult = bgLoop(300.milliseconds) {
                    counter.getAndUpdate { it + 1 }
                    stopWhen(counter.get() == 3)
                }

                // here counter is less than 3 proves that loop is still running in the background
                counter.get() shouldBeLessThan 3
                loopResult.await()
            }

            counter.get() shouldBe 3
            // custom loop interval is 300ms, loop should run 3 times which means it should take around 300*3=900ms
            loopTime shouldBeInRange 900L..1000L
        }
    }

    "waitFor" should {
        "wait for condition to become true before executing rest of the loop body | ESW-89" {
            val numExposures = 5
            var exposureCount = 0
            var exposureReady = false

            // simulate background task which is setting exposureReady flag to true after 200 ms
            launch {
                delay(200)
                exposureReady = true
            }

            loop {
                waitFor { exposureReady }
                exposureReady shouldBe true

                exposureCount += 1
                stopWhen(exposureCount == numExposures)
            }

            numExposures shouldBe 5
        }
    }
})
