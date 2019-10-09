package esw.ocs.scripts.examples.class_based

import csw.params.commands.CommandResponse.Completed
import esw.ocs.dsl.core.Script
import esw.ocs.dsl.script.CswServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Deprecated("Use script based approach to write scripts")
class Script1(cswServices: CswServices) : Script(cswServices) {
    init {
        val eventKey = "csw.a.b."
        fun event(id: Int) = systemEvent("csw.a.b", id.toString())

        handleSetup("command-1") { command ->
            log("============ command-1 ================")

            repeat(50) {
                launch {
                    log("Publishing event $it")
                    delay(1000)
                    publishEvent(event(it))
                    log("Published event $it")
                }
            }

            log("============ command-1 -End ================")
            addOrUpdateCommand(Completed(command.runId()))
        }

        handleSetup("command-2") { command ->
            log("============ command-2 ================")
            val events = getEvent(eventKey + 1)
            log(events.toString())
            events.forEach(::println)

            log("============ command-2 End ================")
            addOrUpdateCommand(Completed(command.runId()))
        }

        handleSetup("command-3") { command ->
            log("============ command-3 ================")

            val keys = (0.until(50)).map { eventKey + it }.toTypedArray()

            onEvent(*keys) { event ->
                println("=======================")
                log("Received: ${event.eventName()}")
            }

            log("============ command-3 End ================")
            addOrUpdateCommand(Completed(command.runId()))
        }

        handleShutdown {
            close()
        }
    }
}
