package me.patrykanuszczyk.traitorbot.commands

import me.patrykanuszczyk.traitorbot.TraitorBot
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.awt.Color
import java.io.CharArrayWriter
import java.io.PrintWriter
import java.lang.Exception
import java.util.concurrent.CompletableFuture

class CommandManager(val bot: TraitorBot) : ListenerAdapter() {
    val commands get() = _commands as Set<Command>

    private var _commands = mutableSetOf<Command>()
    private var _aliasIndex = mutableMapOf<String, AliasIndexEntry>()

    fun registerCommand(command: Command) {
        // Registering name
        val currentCacheForName = _aliasIndex[command.name.toLowerCase()]
        if(currentCacheForName != null) {
            if(currentCacheForName.isMainName)
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
        for(alias in command.aliases) {
            val currentCacheForAlias = _aliasIndex[alias.toLowerCase()]
            if(currentCacheForAlias != null) {
                if(!currentCacheForAlias.isMainName) {
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
            if(event.author.isBot) return

            val (commandName, argsString) = bot.parseCommand(event.message) ?: return
            val suchCommands = getCommandsWithName(commandName)

            when(suchCommands.size) {
                0 -> {
                    event.channel.sendMessage("${event.author.asMention}, nie znalazłem takiej komendy!").submit()
                }
                1 -> {
                    val command = suchCommands.first()
                    command.executor.executeCommand(
                        CommandExecutionArguments(
                            event.message,
                            command,
                            commandName,
                            argsString
                        )
                    )
                }
                else -> {
                    event.channel.sendMessage(
                        "Jest więcej niż jedna komenda o podanym aliasie, ${event.author.asMention}.\n" +
                                "Prosimy użyć właściwej nazwy:\n" +
                                suchCommands.joinToString("\n") { " - `${it.name}`" }
                    ).submit()
                }
            }
        } catch(exception: Exception) {
            val stacktrace = CharArrayWriter()
            PrintWriter(stacktrace).use {
                exception.printStackTrace(it)
            }

            exception.printStackTrace()
            event.channel.sendMessage(MessageBuilder()
                .setContent(event.author.asMention)
                .setEmbed(EmbedBuilder()
                    .setTitle("Wystąpił wyjątek")
                    .setDescription("```\n$stacktrace\n```")
                    .setColor(Color.RED)
                    .build()
                ).build()
            ).submit()
        }
    }
}