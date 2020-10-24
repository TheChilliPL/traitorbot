package me.patrykanuszczyk.traitorbot.commands

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.arguments.CommandInvokeArguments

abstract class Command(val name: String) {
    abstract fun execute(args: CommandInvokeArguments)

    var aliases: Set<String> = emptySet()

    fun hasName(name: String): Boolean {
        if(name.equals(this.name, true)) return true
        return aliases.any { it.equals(name, true) }
    }

    fun andRegister(bot: TraitorBot): Command {
        bot.commandManager.registerCommand(this)
        return this
    }

    fun withAliases(vararg aliases: String): Command {
        this.aliases = aliases.toSet()
        return this
    }

    companion object {
        operator fun invoke(name: String, func: (CommandInvokeArguments) -> Unit): Command {
            return LambdaCommand(name, func)
        }

        operator fun invoke(func: (CommandInvokeArguments) -> Unit): Command = invoke("", func)
    }
}