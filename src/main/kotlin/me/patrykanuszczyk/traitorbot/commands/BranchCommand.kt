package me.patrykanuszczyk.traitorbot.commands

import me.patrykanuszczyk.traitorbot.commands.arguments.CommandInvokeArguments

class BranchCommand(name: String, vararg commands: Command, private var defaultBranch: Command? = null): Command(name) {
    val commands = commands.toSet()

    override fun execute(args: CommandInvokeArguments) {
        val split = args.parameters.split(Regex("""\s+"""), 2)

        val branchName = split.first()

        val branch = commands.firstOrNull { it.hasName(branchName) }

        if(branch != null)
            return branch.execute(args.modify(branch, split.getOrElse(1) {""}))

        executeDefaultBranch(args)
    }

    fun executeDefaultBranch(args: CommandInvokeArguments) {
        defaultBranch ?: LambdaCommand("") {
            it.reply("Nie znaleziono podanej podkomendy.")
        }.execute(args)
    }
}