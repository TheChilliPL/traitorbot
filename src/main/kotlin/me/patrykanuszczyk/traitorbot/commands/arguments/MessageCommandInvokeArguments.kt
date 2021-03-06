package me.patrykanuszczyk.traitorbot.commands.arguments

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.Command
import net.dv8tion.jda.api.entities.Message

class MessageCommandInvokeArguments(bot: TraitorBot, command: Command, val message: Message, parameters: String)
    : DiscordCommandInvokeArguments(bot, command, message.author, parameters) {
    override val channel get() = message.channel

    /**
     * Adds a reaction to the message based on the specified [emoji] parameter.
     *
     * @param [emoji] either a unicode string of the emoji, a codepoint notation, or `name:id` of a custom emote
     */
    fun react(emoji: String) {
        message.addReaction(emoji)
    }

    override fun modify(command: Command, parameters: String): CommandInvokeArguments {
        return MessageCommandInvokeArguments(bot, command, message, parameters)
    }

    override fun hasGlobalPermission(user: String): Boolean {
        return bot.hasGlobalPermission(invoker, user)
    }
}