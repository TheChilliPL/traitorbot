package me.patrykanuszczyk.traitorbot.commands

interface CommandExecutor {
    fun executeCommand(args: CommandExecutionArguments)
}