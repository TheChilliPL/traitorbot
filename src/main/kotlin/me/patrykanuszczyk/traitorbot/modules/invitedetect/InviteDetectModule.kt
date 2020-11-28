package me.patrykanuszczyk.traitorbot.modules.invitedetect

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.arguments.MessageCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.commands.parseParameters
import me.patrykanuszczyk.traitorbot.commands.splitParameters
import me.patrykanuszczyk.traitorbot.modules.BotModule
import me.patrykanuszczyk.traitorbot.utils.PeekableIterator.Companion.peekableIterator
import me.patrykanuszczyk.traitorbot.utils.addUnique
import me.patrykanuszczyk.traitorbot.utils.escapeFormattingInCode
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.entities.Invite as DiscordInvite

class InviteDetectModule(bot: TraitorBot) : BotModule(bot), EventListener {
    val command = Command("invitedetect") { args ->
        val paramsIterator = splitParameters(args.parameters).peekableIterator()
        val param0 = parseParameters(paramsIterator, 1).fail {
            return@Command args.reply(it)
        }.singleOrNull() ?: return@Command
        if(args !is MessageCommandInvokeArguments || !args.isFromGuild)
            return@Command args.reply("Musisz być na serwerze!")
        if(!args.invokerMember!!.hasPermission(Permission.MANAGE_SERVER))
            return@Command bot.commandManager.sendNoPermissionMessage(args)
        when(param0) {
            "enable" -> {
                val param1 = parseParameters(paramsIterator, 1).fail { return@Command args.reply(it) }
                    .singleOrNull()?.removeSurrounding("<#", ">")
                    ?: return@Command args.reply("Musisz podać kanał na wiadomości.")
                val channelId = param1.toLongOrNull()
                val channel = channelId?.let { args.guild!!.getTextChannelById(it) }
                    ?: return@Command args.reply("Nie znaleziono podanego kanału.")
                enableGuild(args.guild!!, channel)
                return@Command args.reply("Włączono.")
            }
            "disable" -> {
                disableGuild(args.guild!!)
                return@Command args.reply("Wyłączono.")
            }
            in setOf("message", "msg") -> {
                val param1 = parseParameters(paramsIterator, 2).fail { return@Command args.reply(it) }
                fun getMessageType(string: String) = when(string.toLowerCase()) {
                    "unknown" -> MessageType.UNKNOWN_INVITER
                    "known" -> MessageType.KNOWN_INVITER
                    else -> null
                }
                when(param1.size) {
                    0 -> return@Command args.reply("Musisz podać minimum jeden parametr.")
                    1 -> {
                        val type = getMessageType(param1[0])
                            ?: return@Command args.reply("Pierwszym parametrem musi być `known` lub `unknown`.")
                        val msg = getMessage(args.guild!!, type).escapeFormattingInCode()
                        args.reply("Ta wiadomość jest obecnie ustawiona na:\n```\n$msg\n```")
                    }
                    2 -> {
                        val type = getMessageType(param1[0])
                            ?: return@Command args.reply("Pierwszym parametrem musi być `known` lub `unknown`.")
                        val msg = param1[1]
                        if(msg.length > 500)
                            return@Command args.reply("Wiadomość nie może być dłuższa niż 500 znaków.")
                        setMessage(args.guild!!, type, msg)
                        val msgEsc = msg.escapeFormattingInCode()
                        args.reply("Ustawiono tę wiadomość na:\n```\n$msgEsc\n```")
                    }
                }
            }
        }
    }.withAliases("invd").andRegister(bot)

    private var cache = mutableMapOf<Long, MutableSet<Invite>>()

    override fun onEvent(event: GenericEvent) {
        if (event !is GenericGuildEvent) return
        if (!isModuleEnabledIn(event.guild)) return
        when (event) {
            is GuildReadyEvent -> {
                val invites = event.guild.retrieveInvites().complete()
                cache[event.guild.idLong] = invites.map { Invite(it) }.toMutableSet()
            }
            is GuildInviteCreateEvent -> {
                cache.computeIfAbsent(event.guild.idLong) { mutableSetOf() }
                    .addUnique(Invite(event.invite))
            }
            is GuildInviteDeleteEvent -> {
                cache[event.guild.idLong]?.removeIf { it.code == event.code }
            }
            is GuildMemberJoinEvent -> {
                val guildCache = cache[event.guild.idLong] ?: return
                val currentInvites = event.guild.retrieveInvites().complete()

                val possibleInvites = guildCache.filter {
                    val curr = currentInvites.find { invite -> invite.code == it.code }

                    if (curr == null) true
                    else curr.uses > it.uses
                }

                val channel = getChannel(event.guild)
                channel?.sendMessage(
                    getMessage(event.guild, event.member, possibleInvites.singleOrNull())
                )?.complete()

                cache[event.guild.idLong] = currentInvites.map { Invite(it) }.toMutableSet()
            }
        }
    }

    init {
        bot.discord.addEventListener(this)
    }

    val guilds: MutableMap<Long, Triple<Long, String, String>>

    init {
        val query = "SELECT guild, channel, join_message_known, join_message_unknown FROM invite_detect"

        bot.database.connection.use { conn ->
            val result = conn.createStatement().executeQuery(query)

            guilds = generateSequence {
                if (!result.next()) null
                else result.getLong("guild") to
                    Triple(
                        result.getLong("channel"),
                        result.getString("join_message_known"),
                        result.getString("join_message_unknown")
                    )
            }.toMap().toMutableMap()
        }
    }

    val defaultKnownMessage = "Dołączył {USER_NAME} z zaproszenia {INVITER_NAME}."
    val defaultUnknownMessage = "Dołączył {USER_NAME} z nieznanego zaproszenia."

    fun enableGuild(guild: Guild, channel: TextChannel) {
        if (channel.guild != guild)
            throw IllegalArgumentException("The specified text channel has to be in the specified guild.")

        val value = guilds.computeIfAbsent(guild.idLong) {
            Triple(0, defaultKnownMessage, defaultUnknownMessage)
        }

        guilds[guild.idLong] = value.copy(first = channel.idLong)

        val query = """
            SET @guild = ?, @channel = ?, @msg1 = ?, @msg2 = ?;
            INSERT INTO invite_detect (guild, channel, join_message_known, join_message_unknown)
                VALUES (@guild, @channel, @msg1, @msg2)
                ON DUPLICATE KEY UPDATE channel = @channel
        """

        bot.database.connection.use { conn ->
            conn.prepareStatement(query).apply {
                setLong(1, guild.idLong)
                setLong(2, channel.idLong)
                setString(3, defaultKnownMessage)
                setString(4, defaultUnknownMessage)
            }.execute()
        }
    }

    fun disableGuild(guild: Guild) {
        val value = guilds[guild.idLong]
        if (value != null) guilds[guild.idLong] = value.copy(first = 0)

        val query = "DELETE FROM invite_detect WHERE guild = ?"

        bot.database.connection.use { conn ->
            conn.prepareStatement(query).apply {
                setLong(1, guild.idLong)
            }.execute()
        }
    }

    enum class MessageType(val column: String) {
        KNOWN_INVITER("join_message_known"),
        UNKNOWN_INVITER("join_message_unknown")
    }

    fun setMessage(guild: Guild, type: MessageType, message: String) {
        val value = guilds[guild.idLong]
        if (value != null) guilds[guild.idLong] = when (type) {
            MessageType.KNOWN_INVITER -> value.copy(second = message)
            MessageType.UNKNOWN_INVITER -> value.copy(third = message)
        }

        val query = "UPDATE invite_detect SET ${type.column} = ? WHERE guild = ?"

        bot.database.connection.use { conn ->
            conn.prepareStatement(query).apply {
                setString(1, message)
                setLong(2, guild.idLong)
            }.execute()
        }
    }

    fun getChannel(guild: Guild): TextChannel? {
        val id = guilds[guild.idLong]?.first ?: return null
        if (id == 0L) return null
        return try {
            guild.getTextChannelById(id)
        } catch (e: Exception) {
            null
        }
    }

    fun getMessage(guild: Guild, member: Member, invite: DiscordInvite?) =
        getMessage(guild, member, invite?.let { Invite(it) })

    private fun getMessage(guild: Guild, member: Member, invite: Invite?): String {
        val original = getMessage(
            guild,
            if (invite != null) MessageType.KNOWN_INVITER
            else MessageType.UNKNOWN_INVITER
        )

        val inviter = if (invite != null) guild.retrieveMemberById(invite.author).complete() else null

        val placeholders = mapOf(
            "USER_MENTION" to member.asMention,
            "USER_NAME" to member.effectiveName,
            "INVITE_CODE" to invite?.code,
            "INVITER_MENTION" to inviter?.asMention,
            "INVITER_NAME" to inviter?.effectiveName
        ).mapValues { it.value ?: "[UNKNOWN]" }

        return placeholders.toList().fold(original) { message, (placeholder, value) ->
            message.replace("{$placeholder}", value)
        }
    }

    fun getMessage(guild: Guild, type: MessageType): String {
        return guilds[guild.idLong]?.run {
            when (type) {
                MessageType.KNOWN_INVITER -> second
                MessageType.UNKNOWN_INVITER -> third
            }
        } ?: getDefaultMessage(type)
    }

    fun getDefaultMessage(type: MessageType) = when (type) {
        MessageType.KNOWN_INVITER -> defaultKnownMessage
        MessageType.UNKNOWN_INVITER -> defaultUnknownMessage
    }

    fun isModuleEnabledIn(guild: Guild) = when (getChannel(guild)?.idLong) {
        in setOf(null, 0L) -> false
        else -> true
    }

    private data class Invite(val code: String, var uses: Int, var maxUses: Int, val author: Long) {
        constructor(invite: DiscordInvite) : this(invite.code, invite.uses, invite.maxUses, invite.inviter!!.idLong)

        override fun equals(other: Any?): Boolean {
            if (other is Invite) {
                return code == other.code
            }
            return super.equals(other)
        }

        override fun hashCode(): Int {
            return code.hashCode()
        }
    }
}