//package me.patrykanuszczyk.traitorbot.modules.voicechatroles
//
//import me.patrykanuszczyk.traitorbot.TraitorBot
//import me.patrykanuszczyk.traitorbot.commands.Command
//import me.patrykanuszczyk.traitorbot.commands.arguments.CommandInvokeArguments
//import me.patrykanuszczyk.traitorbot.modules.BotModule
//import net.dv8tion.jda.api.Permission
//import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
//import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
//import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
//import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
//import net.dv8tion.jda.api.hooks.ListenerAdapter
//import org.jetbrains.exposed.sql.insert
//import org.jetbrains.exposed.sql.select
//import org.jetbrains.exposed.sql.transactions.transaction
//
//class VoicechatRolesModule(bot: TraitorBot) : BotModule(bot), CommandExecutor {
//    val voicechatRoleCommand = Command(
//        "vcroles",
//        setOf("vcrole", "vcr", "voicechatroles", "voicechatrole"),
//        "Pozwala na ustawienie roli dodawanej wszystkim gdy dołączą do wybranego kanału głosowego",
//        this
//    )
//    val listener = Listener(this)
//
//    init {
//        bot.commandManager.registerCommand(voicechatRoleCommand)
//        bot.discord.addEventListener(listener)
//    }
//
//    override fun executeCommand(args: CommandInvokeArguments) {
//        if (args.command != voicechatRoleCommand) return
//
//        if (!args.message.isFromGuild) {
//            args.channel.sendMessage("Tej komendy można używać tylko na serwerze.")
//            return
//        }
//
//        val cmdArgs = args.arguments.split(" ")
//        when (cmdArgs.firstOrNull()) {
//            "list" -> {
//                val roles = transaction {
//                    VoicechatRolesTable.select {
//                        VoicechatRolesTable.guild eq args.guild.idLong
//                    }.map {
//                        it[VoicechatRolesTable.channel] to it[VoicechatRolesTable.role]
//                    }
//                }
//
//                if (roles.isEmpty()) {
//                    args.channel.sendMessage(
//                        "${args.user.asMention}, nie znaleziono żadnych roli VC!"
//                    ).submit()
//                    return
//                }
//
//                args.channel.sendMessage(
//                    roles.joinToString("\n", "Znaleziono następujące role VC:\n") {
//                        (args.guild.getVoiceChannelById(it.first)?.name ?: "${it.first} (not found)") +
//                            " → " +
//                            (args.guild.getRoleById(it.second)?.name ?: "${it.second} (not found)")
//                    }
//                ).submit()
//            }
//            in setOf("add", "set") -> {
//                if (!args.member!!.hasPermission(Permission.MANAGE_SERVER)) {
//                    bot.commandManager.sendNoPermissionMessage(args)
//                    return
//                }
//
//                if (cmdArgs.size < 3) {
//                    args.channel.sendMessage(
//                        "${args.user.asMention}, składnia to:\n" +
//                            "```\nvcrole set id-kanału-głosowego id-roli\n```"
//                    ).submit()
//                    return
//                }
//
//                val vcId = cmdArgs[1].toLong()
//                val roleId = cmdArgs[2].toLong()
//
//                transaction {
//                    VoicechatRolesTable.insert {
//                        it[guild] = args.guild.idLong
//                        it[channel] = vcId
//                        it[role] = roleId
//                    }
//                }
//
//                args.message.addReaction("✅").submit()
//            }
//        }
//    }
//
//    fun processLeave(event: GuildVoiceUpdateEvent) {
//        val channel = event.channelLeft ?: return
//
//        val rolesToRemove = transaction {
//            VoicechatRolesTable.select {
//                VoicechatRolesTable.guild eq channel.guild.idLong
//                VoicechatRolesTable.channel eq channel.idLong
//            }.map { it[VoicechatRolesTable.role] }
//        }
//
//        for (role in rolesToRemove.map { channel.guild.getRoleById(it) }.filterNotNull())
//            channel.guild.removeRoleFromMember(event.entity, role).submit()
//    }
//
//    fun processJoin(event: GuildVoiceUpdateEvent) {
//        val channel = event.channelJoined ?: return
//
//        val rolesToAdd = transaction {
//            VoicechatRolesTable.select {
//                VoicechatRolesTable.guild eq channel.guild.idLong
//                VoicechatRolesTable.channel eq channel.idLong
//            }.map { it[VoicechatRolesTable.role] }
//        }
//
//        for (role in rolesToAdd.map { channel.guild.getRoleById(it) }.filterNotNull())
//            channel.guild.addRoleToMember(event.entity, role).submit()
//    }
//
//    class Listener(val module: VoicechatRolesModule) : ListenerAdapter() {
//        override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
//            module.processJoin(event)
//        }
//
//        override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
//            module.processLeave(event)
//            module.processJoin(event)
//        }
//
//        override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
//            module.processLeave(event)
//        }
//    }
//}