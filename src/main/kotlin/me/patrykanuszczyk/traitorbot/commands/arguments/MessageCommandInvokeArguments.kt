package me.patrykanuszczyk.traitorbot.commands.arguments

import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.utils.guildOrNull
import net.dv8tion.jda.api.entities.Message

class MessageCommandInvokeArguments(command: Command, val message: Message, parameters: String)
    : DiscordCommandInvokeArguments(command, parameters) {
    /**
     * The user that invoked this command.
     */
    val invoker get() = message.author

    val invokerMember get() = message.member

    override val channel get() = message.channel

    /**
     * Adds a reaction to the message based on the specified [emoji] parameter.
     *
     * @param [emoji] either a unicode string of the emoji, a codepoint notation, or `name:id` of a custom emote
     */
    fun react(emoji: String) {
        message.addReaction(emoji)
    }

    val isFromGuild get() = message.isFromGuild

    override fun modify(command: Command, parameters: String): CommandInvokeArguments {
        return MessageCommandInvokeArguments(command, message, parameters)
    }
}