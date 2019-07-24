package esw.ocs.testdata

import csw.params.commands.{CommandIssue, CommandResponse}
import esw.ocs.dsl.Script
import esw.ocs.dsl.CswServices

import scala.concurrent.Future

class TestScript(csw: CswServices) extends Script(csw) {
  handleSetupCommand("command1") { command =>
    println(s"command received: $command")
    csw.crm.addOrUpdateCommand(CommandResponse.Invalid(command.runId, CommandIssue.AssemblyBusyIssue("error")))
    Future.successful(())
  }

  handleSetupCommand("command2") { command =>
    println(s"command received: $command")
    csw.crm.addOrUpdateCommand(CommandResponse.Completed(command.runId))
    Future.successful(())
  }
}
