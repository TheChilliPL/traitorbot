package me.patrykanuszczyk.traitorbot.commands.arguments

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.Command

class ConsoleCommandInvokeArguments(bot: TraitorBot, command: Command, parameters: String)
    : CommandInvokeArguments(bot, command, parameters) {
    override fun reply(message: String) {
        println(message)
    }

    override fun modify(command: Command, parameters: String): ConsoleCommandInvokeArguments {
        return ConsoleCommandInvokeArguments(bot, command, parameters)
    }

    override fun hasGlobalPermission(user: String): Boolean = true
}