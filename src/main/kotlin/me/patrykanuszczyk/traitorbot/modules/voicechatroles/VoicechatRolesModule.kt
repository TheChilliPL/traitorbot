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
import org.jetbrains.exposed.sql.deleteWhere
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
        val exists = transaction {
            !VoicechatRolesTable.select {
                VoicechatRolesTable.guild eq it.guild!!.idLong
            }.limit(1).empty()
        }

        if(exists) {
            return@Command it.reply("Podana rola VC już istnieje.")
        }

        transaction {
            VoicechatRolesTable.insert { row ->
                row[guild] = it.guild!!.idLong
                row[channel] = vc.idLong
                row[this.role] = role.idLong
            }
        }

        it.reply("Pomyślnie utworzono rolę VC. Przy wejściu na kanał ${vc.name} wszyscy dostaną rolę ${role.name}!")
    }.withAliases("add")

    val removeCommand = Command("remove") {
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

        val vcString = parameters[0].removeSurrounding("<#", ">")

        var vcId = vcString.toLongOrNull()

        if(vcId == null) {
            vcId = it.guild!!.voiceChannels.singleOrNull { ch -> ch.name.contains(vcString, true) }?.idLong
                ?: return@Command it.reply("Nie znaleziono takiego kanału VC lub znaleziono więcej niż jeden.")
        }

        val roleString = parameters[1].removeSurrounding("<@&", ">")

        var roleId = roleString.toLongOrNull()

        if(roleId == null) {
            roleId = it.guild!!.roles.singleOrNull { r -> r.name.contains(roleString, true) }?.idLong
                ?: return@Command it.reply("Nie znaleziono takiej roli lub znaleziono więcej niż jedną.")
        }

        // Removing
        val exists = transaction {
            !VoicechatRolesTable.select {
                VoicechatRolesTable.guild.eq(it.guild!!.idLong) and
                    VoicechatRolesTable.channel.eq(vcId) and
                    VoicechatRolesTable.role.eq(roleId)
            }.limit(1).empty()
        }

        if(!exists) {
            return@Command it.reply("Podana rola VC nie istnieje.")
        }

        transaction {
            VoicechatRolesTable.deleteWhere {
                VoicechatRolesTable.guild.eq(it.guild!!.idLong) and
                    VoicechatRolesTable.channel.eq(vcId) and
                    VoicechatRolesTable.role.eq(roleId)
            }
        }

        it.reply("Usunięto podaną rolę VC!")
    }.withAliases("del", "delete")

    val rootCommand = BranchCommand(
        "vcroles",
        listCommand, setCommand, removeCommand
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
    fun getVoicechatRoles(guild: Guild): List<Pair<Long, Long>> {
        return transaction {
            VoicechatRolesTable.select {
                VoicechatRolesTable.guild eq guild.idLong
            }.map {
                it[VoicechatRolesTable.channel] to it[VoicechatRolesTable.role]
            }.toList()
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