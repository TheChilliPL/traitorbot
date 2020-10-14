package me.patrykanuszczyk.traitorbot.commands.arguments

import me.patrykanuszczyk.traitorbot.commands.Command

class ConsoleCommandInvokeArguments(command: Command, parameters: String) : CommandInvokeArguments(command, parameters) {
    override fun reply(message: String) {
        println(message)
    }

    override fun modify(command: Command, parameters: String): ConsoleCommandInvokeArguments {
        return ConsoleCommandInvokeArguments(command, parameters)
    }
}