package me.patrykanuszczyk.traitorbot.commands.arguments

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.utils.guildOrNull
import me.patrykanuszczyk.traitorbot.utils.maybeAs
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.PrivateChannel

/**
 * Represents a command invocation that takes place on Discord.
 */
abstract class DiscordCommandInvokeArguments(bot: TraitorBot, command: Command, parameters: String)
    : CommandInvokeArguments(bot, command, parameters) {
    /**
     * The channel in which the command was invoked.
     * It might be the channel of a message sent, of a reaction, or where a timer was initiated.
     */
    abstract val channel: MessageChannel

    override fun reply(message: String) {
        channel.sendMessage(message).complete()
    }

    /**
     * The guild in which the command was invoked.
     *
     * If the command was invoked in a [PrivateChannel], returns `null`.
     */
    val guild get() = (channel maybeAs GuildChannel::class)?.guild
}