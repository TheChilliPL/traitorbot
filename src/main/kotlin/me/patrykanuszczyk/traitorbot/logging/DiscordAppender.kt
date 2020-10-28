package me.patrykanuszczyk.traitorbot.logging

import me.patrykanuszczyk.traitorbot.TraitorBot
import net.dv8tion.jda.api.JDA
import org.apache.logging.log4j.LogManager.getLogger
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import java.io.Serializable

@Plugin(
    name = "DiscordAppender",
    category = "Core",
    elementType = Appender.ELEMENT_TYPE
)
class DiscordAppender(
    name: String,
    filter: Filter?,
    layout: Layout<out Serializable>?,
    val guildId: String,
    val channelId: String
) : AbstractAppender(name, filter, layout, false, null) {
    private var noChannelMessageShown = false

    override fun append(event: LogEvent) {
        try {
            val discord = TraitorBot.instance?.discord ?: return
            //discord.awaitReady()
            if (discord.status != JDA.Status.CONNECTED) return
            val channel = discord.getGuildById(guildId)?.getTextChannelById(channelId)

            if (channel == null) {
                if (!noChannelMessageShown) {
                    noChannelMessageShown = true

                    val logger = getLogger(DiscordAppender::class)
                    logger.error("Couldn't find text channel with the specified ID.")
                }
                return
            }

            var message = String(layout.toByteArray(event))

            //println(message)

            if (message.length > 2000) {
                message = message.take(1999) + "…"
            }

            channel.sendMessage(message).queue()
        } catch (exception: Exception) {
        }
    }

    companion object {
        @JvmStatic
        @PluginFactory
        fun createAppender(
            @PluginAttribute("name") name: String,
            @PluginElement("Filters") filter: Filter?,
            @PluginElement("Layout") layout: Layout<out Serializable>,
            @PluginAttribute("guild") guild: String,
            @PluginAttribute("channel") channel: String
        ): DiscordAppender {
            return DiscordAppender(name, filter, layout, guild, channel)
        }
    }
}