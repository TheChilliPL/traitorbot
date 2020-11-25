package me.patrykanuszczyk.traitorbot.modules.voicechatroles

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.BranchCommand
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.arguments.DiscordCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.commands.parseParameters
import me.patrykanuszczyk.traitorbot.modules.BotModule
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.EventListener

class VoicechatRolesModule(bot: TraitorBot) : BotModule(bot), EventListener {
    val listCommand = Command("list") {
        if (it !is DiscordCommandInvokeArguments || !it.isFromGuild)
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
        if (it !is DiscordCommandInvokeArguments || !it.isFromGuild)
            return@Command it.reply("Musisz być na serwerze aby użyć tej komendy!")

        if (!it.invokerMember!!.hasPermission(Permission.MANAGE_SERVER))
            return@Command bot.commandManager.sendNoPermissionMessage(it)

        val parameters = parseParameters(it.parameters).fail { f ->
            return@Command it.reply(f)
        }

        if (parameters.size < 2)
            return@Command it.reply("Musisz podać kanał głosowy i rolę.")

        // Parsing voice channel
        var vc: VoiceChannel?

        val vcString = parameters[0].removeSurrounding("<#", ">")

        vc = vcString.toLongOrNull()?.let { id -> it.guild!!.getVoiceChannelById(id) }

        if (vc == null) {
            vc = it.guild!!.voiceChannels.singleOrNull { ch -> ch.name.contains(vcString, true) }
                ?: return@Command it.reply("Nie znaleziono takiego kanału VC lub znaleziono więcej niż jeden.")
        }

        // Parsing role
        var role: Role?

        val roleString = parameters[1].removeSurrounding("<@&", ">")

        role = roleString.toLongOrNull()?.let { id -> it.guild!!.getRoleById(id) }

        if (role == null) {
            role = it.guild!!.roles.singleOrNull { r -> r.name.contains(roleString, true) }
                ?: return@Command it.reply("Nie znaleziono takiej roli lub znaleziono więcej niż jedną.")
        }

        // Adding
        val existed = !setVoicechatRole(vc, role)

        if (existed) {
            return@Command it.reply("Podana rola VC już istnieje.")
        }

        it.reply(
            "Pomyślnie utworzono rolę VC." +
                "Przy wejściu na kanał **${vc.name}** wszyscy dostaną rolę **${role.name}**!"
        )
    }.withAliases("add")

    val removeCommand = Command("remove") {
        if (it !is DiscordCommandInvokeArguments || !it.isFromGuild)
            return@Command it.reply("Musisz być na serwerze aby użyć tej komendy!")

        if (!it.invokerMember!!.hasPermission(Permission.MANAGE_SERVER))
            return@Command bot.commandManager.sendNoPermissionMessage(it)

        val parameters = parseParameters(it.parameters).fail { f ->
            return@Command it.reply(f)
        }

        if (parameters.size < 2)
            return@Command it.reply("Musisz podać kanał głosowy i rolę.")

        val vcString = parameters[0].removeSurrounding("<#", ">")

        var vcId = vcString.toLongOrNull()

        if (vcId == null) {
            vcId = it.guild!!.voiceChannels.singleOrNull { ch -> ch.name.contains(vcString, true) }?.idLong
                ?: return@Command it.reply("Nie znaleziono takiego kanału VC lub znaleziono więcej niż jeden.")
        }

        val roleString = parameters[1].removeSurrounding("<@&", ">")

        var roleId = roleString.toLongOrNull()

        if (roleId == null) {
            roleId = it.guild!!.roles.singleOrNull { r -> r.name.contains(roleString, true) }?.idLong
                ?: return@Command it.reply("Nie znaleziono takiej roli lub znaleziono więcej niż jedną.")
        }

        // Removing
        val exists = removeVoicechatRole(it.guild!!.idLong, vcId, roleId)

        if (!exists) {
            return@Command it.reply("Podana rola VC nie istnieje.")
        }

        it.reply("Usunięto podaną rolę VC!")
    }.withAliases("del", "delete")

    val rootCommand = BranchCommand(
        "vcroles",
        listCommand, setCommand, removeCommand
    ).withAliases("vcr").andRegister(bot)

    override fun onEvent(event: GenericEvent) {
        if (event !is GuildVoiceUpdateEvent) return

        applyRoles(event.entity, event.channelLeft, event.channelJoined)
    }

    init {
        bot.discord.addEventListener(this)
    }

    private var voicechatRoles: MutableMap<Long, MutableMap<Long, MutableSet<Long>>>? = null

    init {
        val query = "SELECT guild, channel, role FROM voicechat_roles"

        bot.database.connection.use { conn ->
            val result = conn.createStatement().executeQuery(query)
            voicechatRoles = generateSequence {
                if (!result.next()) null
                else Triple(
                    result.getLong("guild"),
                    result.getLong("channel"),
                    result.getLong("role")
                )
            }.groupBy({ it.first }, { it.second to it.third })
                .mapValues {
                    it.value.groupBy({ it.first }, { it.second })
                        .mapValues { it.value.toMutableSet() }
                        .toMap().toMutableMap()
                }
                .toMutableMap()
        }
    }

    /**
     * Gets VC roles for all the voice channels in the specified [guild].
     *
     * @return set of pairs, where the first element is channel ID, and the second is role ID
     */
    fun getVoicechatRoles(guild: Guild): List<Pair<Long, Long>> {
        return voicechatRoles!![guild.idLong]?.toList()?.flatMap { it.second.map { a -> it.first to a } } ?: emptyList()
    }

    /**
     * Gets VC roles for the specified voice [channel].
     *
     * @return set of role IDs.
     */
    fun getVoicechatRoles(channel: VoiceChannel): Set<Long> {
        return voicechatRoles!![channel.guild.idLong]?.get(channel.idLong) ?: emptySet()
    }

    /**
     * Adds the specified voicechat role.
     *
     * @return `true` if the role was added; `false` if it had already existed.
     */
    fun setVoicechatRole(channel: VoiceChannel, role: Role): Boolean {
        try {
            return voicechatRoles!!.computeIfAbsent(channel.guild.idLong) { mutableMapOf() }
                .computeIfAbsent(channel.idLong) { mutableSetOf() }
                .add(role.idLong)
        } finally {
            val query = "INSERT INTO voicechat_roles (guild, channel, role) VALUES (?, ?, ?)"
            bot.database.connection.prepareStatement(query).apply {
                setLong(1, channel.guild.idLong)
                setLong(2, channel.idLong)
                setLong(3, role.idLong)
            }
        }
    }

    /**
     * Removes the specified voicechat role.
     *
     * @return `true` if the role was removed; `false` if it didn't exist.
     */
    fun removeVoicechatRole(channel: VoiceChannel, role: Role): Boolean {
        return removeVoicechatRole(channel.guild.idLong, channel.idLong, role.idLong)
    }

    /**
     * Removes the specified voicechat role.
     *
     * @return `true` if the role was removed; `false` if it didn't exist.
     */
    fun removeVoicechatRole(guildId: Long, channelId: Long, roleId: Long): Boolean {
        try {
            return voicechatRoles!![guildId]?.get(channelId)?.remove(roleId) ?: false
        } finally {
            val query = "DELETE IGNORE FROM voicechat_roles WHERE guild = ? AND channel = ? AND role = ?"
            bot.database.connection.prepareStatement(query).apply {
                setLong(1, guildId)
                setLong(2, channelId)
                setLong(3, roleId)
            }.execute()
        }
    }

    fun applyRoles(member: Member, channelLeft: VoiceChannel?, channelJoined: VoiceChannel?) {
        val guild = member.guild

        val roleIdsToAdd = if (channelJoined != null)
            getVoicechatRoles(channelJoined)
        else
            emptySet()

        val roleIdsToRemove = if (channelLeft != null)
            getVoicechatRoles(channelLeft).filter { it !in roleIdsToAdd }.toSet()
        else
            emptySet()

        val rolesToAdd = roleIdsToAdd.map { guild.getRoleById(it) }

        val rolesToRemove = roleIdsToRemove.map { guild.getRoleById(it) }

        guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove).complete()
    }
}