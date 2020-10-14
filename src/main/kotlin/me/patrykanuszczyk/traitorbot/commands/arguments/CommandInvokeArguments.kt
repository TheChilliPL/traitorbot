package me.patrykanuszczyk.traitorbot.commands.arguments

import me.patrykanuszczyk.traitorbot.commands.Command

abstract class CommandInvokeArguments(
    /**
     * The command that got invoked.
     */
    val command: Command,
    val parameters: String
) {
    /**
     * Replies with a text to the command invoker.
     */
    abstract fun reply(message: String)

    abstract fun modify(command: Command, parameters: String): CommandInvokeArguments

    open fun hasGlobalPermission(string: String): Boolean = false
}