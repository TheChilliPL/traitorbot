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
    private var _commands = mutableSetOf<Command>()
    val commands get() = _commands as Set<Command>
    private var _aliasIndex = mutableMapOf<String, AliasIndexEntry>()//MutablePair<MutableSet<Command>, Boolean>>()

    fun registerCommand(command: Command) {
        // Registering name
        val currentCacheForName = _aliasIndex[command.name.toLowerCase()]
        if(currentCacheForName != null) {
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
//            fun noPermission() {
//                event.channel.sendMessage(":knife: ${event.author.asMention}, nie masz na to permisji!")
//                return
//            }
            if(event.author.isBot) return

            val (commandName, argsString) = bot.parseCommand(event.message) ?: return

            println("CMD: $commandName, $argsString")

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
                            suchCommands.map { " - `${it.name}`" }.joinToString("\n")
                    ).submit()
                }
            }

            /*when (commandName) {
                "stop" -> {
                    val hasPermission = bot.hasGlobalPermission(event.author, "bot_stop")

                    if (!hasPermission) return noPermission()

                    event.channel.sendMessage(":ok_hand: Już się robi, ${event.author.asMention}!").submit()
                    bot.discord.presence.setStatus(OnlineStatus.OFFLINE)
                    bot.discord.shutdown()
                    return
                }
                "prefix" -> {
                    val args = argsString.split(" ", limit = 2)
                    when (args[0]) {
                        "set" -> {
                            if (args.size <= 1 || args[1].isEmpty()) {
                                event.channel.sendMessage("${event.author.asMention}, prefix nie może być pusty.")
                                    .submit()
                                return;
                            }
                            transaction {
                                GuildPrefixTable.insertOrUpdate(
                                    GuildPrefixTable.prefix
                                ) {
                                    it[id] = event.guild.idLong
                                    it[prefix] = args[1]
                                }
                            }
                            event.channel.sendMessage("${event.author.asMention}, prefix serwera zmieniono!")
                                .submit()
                        }
                        in listOf("remove", "delete", "del") -> {
                            transaction {
                                GuildPrefixTable.deleteIgnoreWhere {
                                    GuildPrefixTable.id eq event.guild.idLong
                                }
                            }
                            event.channel.sendMessage("${event.author.asMention}, prefix został usunięty!")
                                .submit()
                        }
                        else -> {
                            val prefix = bot.getPrefixFor(event.guild)
                            if(prefix == null) {
                                event.channel.sendMessage(
                                    "${event.author.asMention}, obecnie nie ma ustawionego prefixu " +
                                        "dla tego serwera.\nMożesz go ustawić korzystając z komendy " +
                                        "`prefix set <prefix>`.").submit()
                                return
                            }
                            event.channel.sendMessage("${event.author.asMention}, obecny prefix to `$prefix`")
                                .submit()
                        }
                    }
                }
                "vcrole" -> {
                    val args = argsString.split(" ")
                    when(args[0]) {
                        "list" -> {
                            val roles = transaction {
                                VoicechatRolesTable.select {
                                    VoicechatRolesTable.guild eq event.guild.idLong
                                }.map {
                                    it[VoicechatRolesTable.channel] to it[VoicechatRolesTable.role]
                                }
                            }

                            if(roles.isEmpty()) {
                                event.channel.sendMessage(
                                    "${event.author.asMention}, nie znaleziono żadnych roli VC!"
                                ).submit()
                                return
                            }

                            event.channel.sendMessage(
                                roles.joinToString("\n", "Znaleziono następujące role VC:\n") {
                                    (event.guild.getVoiceChannelById(it.first)?.name ?: "${it.first} (not found)") +
                                        " → " +
                                        (event.guild.getRoleById(it.second)?.name ?: "${it.second} (not found)")
                                }
                            ).submit()
                        }
                    }
                }
            }*/
        } catch(e: Exception) {
            println("Exception occurred")
            val cw = CharArrayWriter()
            PrintWriter(cw).use {
                e.printStackTrace(it)
            }
            val stackTrace = cw.toString()
            println(stackTrace)
            event.channel.sendMessage(MessageBuilder()
                .setContent(event.author.asMention)
                .setEmbed(EmbedBuilder()
                    .setTitle("Wystąpił wyjątek")
                    .setDescription("```\n$stackTrace\n```")
                    .setColor(Color.RED)
                    .build()
                ).build()
            ).submit()
        }
    }
}