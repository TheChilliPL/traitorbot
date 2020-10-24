package me.patrykanuszczyk.traitorbot.commands

import me.patrykanuszczyk.traitorbot.commands.arguments.CommandInvokeArguments

class LambdaCommand(name: String, private val func: (CommandInvokeArguments) -> Unit): Command(name) {
    override fun execute(args: CommandInvokeArguments) {
        func(args)
    }
}