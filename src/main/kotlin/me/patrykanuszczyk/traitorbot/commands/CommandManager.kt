package me.patrykanuszczyk.traitorbot.commands

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.arguments.CommandInvokeArguments
import me.patrykanuszczyk.traitorbot.commands.arguments.MessageCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.utils.guildOrNull
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.io.CharArrayWriter
import java.io.PrintWriter

class CommandManager(val bot: TraitorBot) {
    val logger: Logger = LogManager.getLogger(CommandManager::class.java)

    val commands get() = _commands as Set<Command>
    private var _commands = mutableSetOf<Command>()

    fun registerCommand(command: Command) {
        _commands.add(command)
        logger.info("Registered command ${command.name}")
    }

    fun getCommandsWithName(name: String): Set<Command> = commands.filter { it.hasName(name) }.toSet()

    fun sendNoPermissionMessage(arguments: CommandInvokeArguments) {
        arguments.reply(
            ":knife: Nie masz na to permisji!"
        )
    }

    fun onMessageReceived(event: MessageReceivedEvent): Boolean {
        try {
            if (event.author.isBot) return false

            val (name, args) = parseCommandMessage(event.message) ?: return false

            executeCommand(event.message, name, args)

            return true
        } catch (exception: Exception) {
            handleThrowable(event, exception)

            return true
        }
    }

    fun handleThrowable(event: MessageReceivedEvent, throwable: Throwable) {
        logger.error("Exception was thrown.", throwable)

        val stacktrace = CharArrayWriter()
        PrintWriter(stacktrace).use {
            throwable.printStackTrace(it)
        }
        val stackTraceLines = stacktrace.toString().lineSequence()
        val stackTraceCropped = StringBuilder(stacktrace.size().coerceAtMost(2000))
        stackTraceCropped.append("```\n")
        var lengthRemaining = 2000 - 9
        for(line in stackTraceLines) {
            lengthRemaining -= line.length + 1
            if(lengthRemaining < 0) {
                stackTraceCropped.append("…")
                break
            }
            stackTraceCropped.append(line + "\n")
        }
        stackTraceCropped.append("\n```")
        event.channel.sendMessage(
            MessageBuilder()
                .setContent(event.author.asMention)
                .setEmbed(
                    EmbedBuilder()
                        .setTitle("Wystąpił wyjątek")
                        .setDescription(stackTraceCropped)
                        .setColor(Color.RED)
                        .build()
                ).build()
        ).queue()
    }

    fun executeCommand(message: Message, name: String, args: String) {
        val commands = getCommandsWithName(name)

        logger.info("Executing command: $name $args")

        when(commands.size) {
            0 -> message.channel.sendMessage("${message.author.asMention}, nie znalazłem takiej komendy!").queue()
            1 -> {
                val command = commands.first()
                command.execute(MessageCommandInvokeArguments(
                    bot, command, message, args
                ))
            }
            2 -> message.channel.sendMessage(
                "Jest więcej niż jedna komenda o podanym aliasie, ${message.author.asMention}.\n" +
                    "Prosimy użyć właściwej nazwy:\n" +
                    commands.joinToString("\n") { " - `${it.name}`" }
            ).queue()
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
            if(content.startsWith(prefix)) return content.substring(prefix.length).trim()
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