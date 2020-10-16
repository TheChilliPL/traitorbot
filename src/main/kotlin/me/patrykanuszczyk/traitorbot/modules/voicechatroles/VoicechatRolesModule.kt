package me.patrykanuszczyk.traitorbot.modules.voicechatroles

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.BranchCommand
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.CommandManager
import me.patrykanuszczyk.traitorbot.commands.arguments.DiscordCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.commands.parseParameters
import me.patrykanuszczyk.traitorbot.modules.BotModule
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class VoicechatRolesModule(bot: TraitorBot) : BotModule(bot), EventListener {
    val listCommand = Command("list") {
        if(it !is DiscordCommandInvokeArguments || !it.isFromGuild)
            return@Command it.reply("Musisz być na serwerze aby użyć tej komendy!")

        val guild = it.guild!!

        val roles = getVoicechatRoles(guild)

        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle("Obecnie ustawione role VC.")
                .apply {
                    roles.map { vcRole ->
                        val channel = guild.getVoiceChannelById(vcRole.first)?.name
                            ?: "Nieznany kanał (${vcRole.first})"
                        val role = guild.getRoleById(vcRole.second)?.asMention
                            ?: "nieznana rola (${vcRole.second})"
                        channel to role
                    }.takeUnless { r -> r.isEmpty() }?.forEach { (channel, role) ->
                        addField(channel, role, true)
                    } ?: setDescription("Brak ról VC na serwerze.")
                }
                .build()
        ).complete()
    }

    val setCommand = Command("set") {
        if(it !is DiscordCommandInvokeArguments || !it.isFromGuild)
            return@Command it.reply("Musisz być na serwerze aby użyć tej komendy!")

        if(!it.invokerMember!!.hasPermission(Permission.MANAGE_SERVER))
            return@Command bot.commandManager.sendNoPermissionMessage(it)

        val parse = parseParameters(it.parameters)

        if(parse.failed)
            return@Command it.reply(parse.failValue!!)

        val parameters = parse.successValue!!

        if(parameters.size < 2)
            return@Command it.reply("Musisz podać kanał głosowy i rolę.")

        // Parsing voice channel
        var vc: VoiceChannel?

        val vcString = parameters[0].removeSurrounding("<#", ">")

        vc = vcString.toLongOrNull()?.let { id -> it.guild!!.getVoiceChannelById(id) }

        if(vc == null) {
            vc = it.guild!!.voiceChannels.singleOrNull { ch -> ch.name.contains(vcString, true) }
                ?: return@Command it.reply("Nie znaleziono takiego kanału VC lub znaleziono więcej niż jeden.")
        }

        // Parsing role
        var role: Role?

        val roleString = parameters[1].removeSurrounding("<@&", ">")

        role = roleString.toLongOrNull()?.let { id -> it.guild!!.getRoleById(id) }

        if(role == null) {
            role = it.guild!!.roles.singleOrNull { r -> r.name.contains(roleString, true) }
                ?: return@Command it.reply("Nie znaleziono takiej roli lub znaleziono więcej niż jedną.")
        }

        // Adding
        transaction {
            VoicechatRolesTable.insert { row ->
                row[guild] = it.guild!!.idLong
                row[channel] = vc.idLong
                row[this.role] = role.idLong
            }
        }

        it.reply("Pomyślnie utworzono rolę VC. Przy wejściu na kanał ${vc.name} wszyscy dostaną rolę ${role.name}!")
    }.withAliases("add")

    val rootCommand = BranchCommand(
        "vcroles",
        listCommand, setCommand
    ).withAliases("vcr").andRegister(bot)

    override fun onEvent(event: GenericEvent) {
        if(event !is GuildVoiceUpdateEvent) return

        applyRoles(event.entity, event.channelLeft, event.channelJoined)
    }

    init {
        bot.discord.addEventListener(this)
    }

    /**
     * Gets VC roles for all the voice channels in the specified [guild].
     *
     * @return set of pairs, where the first element is channel ID, and the second is role ID
     */
    fun getVoicechatRoles(guild: Guild): Set<Pair<Long, Long>> {
        return transaction {
            VoicechatRolesTable.select {
                VoicechatRolesTable.guild eq guild.idLong
            }.map {
                it[VoicechatRolesTable.channel] to it[VoicechatRolesTable.role]
            }.toSet()
        }
    }

    /**
     * Gets VC roles for the specified voice [channel] in the specified [guild].
     *
     * @return set of role IDs
     */
    fun getVoicechatRoles(guild: Guild, channel: VoiceChannel): Set<Long> {
        return transaction {
            VoicechatRolesTable.select {
                VoicechatRolesTable.guild.eq(guild.idLong) and
                    VoicechatRolesTable.channel.eq(channel.idLong)
            }.map {
                it[VoicechatRolesTable.role]
            }.toSet()
        }
    }

    fun applyRoles(member: Member, channelLeft: VoiceChannel?, channelJoined: VoiceChannel?) {
        val guild = member.guild

        val roleIdsToAdd = if(channelJoined != null)
            getVoicechatRoles(guild, channelJoined)
        else
            setOf()

        val roleIdsToRemove = if(channelLeft != null)
            getVoicechatRoles(guild, channelLeft).filter { it !in roleIdsToAdd }.toSet()
        else
            setOf()

        val rolesToAdd = roleIdsToAdd.map { guild.getRoleById(it) }

        val rolesToRemove = roleIdsToRemove.map { guild.getRoleById(it) }

        guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove).complete()
    }
}

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