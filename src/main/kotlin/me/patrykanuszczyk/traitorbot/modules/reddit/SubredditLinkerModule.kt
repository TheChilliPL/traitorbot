package me.patrykanuszczyk.traitorbot.modules.reddit

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.arguments.DiscordCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.modules.BotModule
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class SubredditLinkerModule(bot: TraitorBot) : BotModule(bot) {
    val command = Command("subreddit_linker") {
        if (it !is DiscordCommandInvokeArguments || !it.isFromGuild)
            return@Command it.reply("Ta komenda musi być wywołana na serwerze.")

        when (it.parameters.toLowerCase().trim()) {
            in setOf("enable", "on") -> {
                if (!it.invokerMember!!.hasPermission(Permission.MANAGE_SERVER))
                    return@Command bot.commandManager.sendNoPermissionMessage(it)
                val result = switchSubredditLinker(it.guild!!, true)
                it.reply(
                    if (result)
                        "Włączono linkowanie subredditów na tym serwerze."
                    else
                        "Linkowanie subredditów na tym serwerze jest już włączone."
                )
            }
            in setOf("disable", "off") -> {
                if (!it.invokerMember!!.hasPermission(Permission.MANAGE_SERVER))
                    return@Command bot.commandManager.sendNoPermissionMessage(it)
                val result = switchSubredditLinker(it.guild!!, false)
                it.reply(
                    if (result)
                        "Wyłączono linkowanie subredditów na tym serwerze."
                    else
                        "Linkowanie subredditów na tym serwerze jest już wyłączone."
                )
            }
            else -> {
                it.reply(
                    "Linkowanie subredditów na tym serwerze jest **" +
                        (if (isSubredditLinkerEnabled(it.guild!!))
                            ":white_check_mark: włączone"
                        else
                            ":x: wyłączone") +
                        "**."
                )
            }
        }
    }.withAliases("subredditlinker", "srlink").andRegister(bot)

    private var linkerEnabledGuilds: MutableSet<Long>

    init {
        val query = "SELECT guild FROM subreddit_linker"

        bot.database.connection.use { conn ->
            val result = conn.createStatement().executeQuery(query)

            linkerEnabledGuilds = generateSequence {
                if (!result.next()) null
                else
                    result.getLong("guild")
            }.toMutableSet()
        }
    }

    fun isSubredditLinkerEnabled(guild: Guild): Boolean {
        return guild.idLong in linkerEnabledGuilds
    }

    fun switchSubredditLinker(guild: Guild, enable: Boolean): Boolean {
        val (result, query) = if (enable) {
            linkerEnabledGuilds.add(guild.idLong) to
                "INSERT IGNORE INTO subreddit_linker (guild) VALUES (?)"
        } else {
            linkerEnabledGuilds.remove(guild.idLong) to
                "DELETE IGNORE FROM subreddit_linker WHERE guild = ?"
        }

        bot.database.connection.use { conn ->
            conn.prepareStatement(query).apply {
                setLong(1, guild.idLong)
            }.execute()
        }

        return result
    }

    override fun onMessageReceived(event: MessageReceivedEvent): Boolean {
        if (!event.isFromGuild
            || event.author.isBot
            || !isSubredditLinkerEnabled(event.guild)
        )
            return false

        val matches = subredditRegex
            .findAll(event.message.contentStripped)
            .take(5)

        if (matches.none()) return false

        event.message.reply(
            EmbedBuilder()
                .setTitle("Znaleziono we wiadomości następujące subreddity:")
                .apply {
                    matches.forEach {
                        val subreddit = it.groups[1]?.value
                        appendDescription("[r/$subreddit](https://www.reddit.com/r/$subreddit/)\n")
                    }
                }
                .build()
        ).mentionRepliedUser(false).complete()

        return false
    }

    companion object {
        val subredditRegex = Regex(
            """\br/([a-z0-9]\w{2,20})\b""",
            RegexOption.IGNORE_CASE
        )
    }
}