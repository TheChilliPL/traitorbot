package me.patrykanuszczyk.traitorbot.commands

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.utils.guildOrNull
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.awt.Color
import java.io.CharArrayWriter
import java.io.PrintWriter
import java.util.concurrent.CompletableFuture

class CommandManager(val bot: TraitorBot) : ListenerAdapter() {
    val commands get() = _commands as Set<Command>

    private var _commands = mutableSetOf<Command>()
    private var _aliasIndex = mutableMapOf<String, AliasIndexEntry>()

    fun registerCommand(command: Command) {
        // Registering name
        val currentCacheForName = _aliasIndex[command.name.toLowerCase()]
        if (currentCacheForName != null) {
            if (currentCacheForName.isMainName)
                throw UnsupportedOperationException("Command of that name already exists")
            else {
                currentCacheForName.commands = mutableSetOf(command)
                currentCacheForName.isMainName = true
            }
        } else {
            _aliasIndex[command.name.toLowerCase()] = AliasIndexEntry(mutableSetOf(command), true)
        }

        _commands.add(command)

        // Registering aliases
        for (alias in command.aliases) {
            val currentCacheForAlias = _aliasIndex[alias.toLowerCase()]
            if (currentCacheForAlias != null) {
                if (!currentCacheForAlias.isMainName) {
                    currentCacheForAlias.commands.add(command)
                }
            } else {
                _aliasIndex[alias.toLowerCase()] = AliasIndexEntry(mutableSetOf(command), false)
            }
        }
    }

    fun getCommandsWithName(name: String): Set<Command> = _aliasIndex[name]?.commands ?: emptySet()

    fun sendNoPermissionMessage(arguments: CommandExecutionArguments): CompletableFuture<Message> {
        return arguments.message.channel.sendMessage(
            ":knife: ${arguments.message.author.asMention}, nie masz na to permisji!"
        ).submit()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        try {
            if (event.author.isBot) return

            val (name, args) = parseCommandMessage(event.message) ?: return

            executeCommand(event.message, name, args)
        } catch (exception: Exception) {
            exception.printStackTrace()

            val stacktrace = CharArrayWriter()
            PrintWriter(stacktrace).use {
                exception.printStackTrace(it)
            }
            event.channel.sendMessage(
                MessageBuilder()
                    .setContent(event.author.asMention)
                    .setEmbed(
                        EmbedBuilder()
                            .setTitle("Wystąpił wyjątek")
                            .setDescription("```\n$stacktrace\n```")
                            .setColor(Color.RED)
                            .build()
                    ).build()
            ).submit()
        }
    }

    fun executeCommand(message: Message, name: String, args: String) {
        val commands = getCommandsWithName(name)

        when(commands.size) {
            0 -> message.channel.sendMessage("${message.author.asMention}, nie znalazłem takiej komendy!").submit()
            1 -> {
                val command = commands.first()
                command.executor.executeCommand(
                    CommandExecutionArguments(
                        message,
                        command,
                        name,
                        args
                    )
                )
            }
            2 -> message.channel.sendMessage(
                "Jest więcej niż jedna komenda o podanym aliasie, ${message.author.asMention}.\n" +
                    "Prosimy użyć właściwej nazwy:\n" +
                    commands.joinToString("\n") { " - `${it.name}`" }
            ).submit()
        }
    }

    fun parsePrefixedMessage(message: Message): String? {
        val content = message.contentRaw.trim()
        val botUserId = bot.discord.selfUser.id

        val prefixes = listOf(
            "<@$botUserId>", "<@!$botUserId>",
            bot.getPrefixFor(message.guildOrNull)
        )

        for(prefix in prefixes) {
            if(prefix == null) continue
            if(content.startsWith(prefix)) return content.substring(prefix.length)
        }

        return null
    }

    fun parseCommandMessage(message: Message): Pair<String, String>? {
        val content = parsePrefixedMessage(message) ?: return null

        return parseCommand(content)
    }

    fun parseCommand(content: String): Pair<String, String> {
        val split = content.split(' ', limit = 2)

        val command = split.first()
        val args = split.getOrNull(1)?.trimStart() ?: ""

        return command to args
    }
}