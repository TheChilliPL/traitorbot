package me.patrykanuszczyk.traitorbot.commands

import net.dv8tion.jda.api.entities.Message

class CommandExecutionArguments(
    val message: Message,
    val command: Command,
    val alias: String,
    val arguments: String
) {
    val user get() = message.author
    val member get() = message.member
    val channel get() = message.channel
    val guild get() = message.guild
}