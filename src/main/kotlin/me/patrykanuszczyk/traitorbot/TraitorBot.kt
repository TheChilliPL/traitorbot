package me.patrykanuszczyk.traitorbot

import me.patrykanuszczyk.traitorbot.admin.AdminModule
import me.patrykanuszczyk.traitorbot.commands.CommandManager
import me.patrykanuszczyk.traitorbot.modules.BotModule
import me.patrykanuszczyk.traitorbot.permissions.GlobalPermission
import me.patrykanuszczyk.traitorbot.permissions.GlobalPermissionsTable
import me.patrykanuszczyk.traitorbot.prefix.GuildPrefix
import me.patrykanuszczyk.traitorbot.prefix.GuildPrefixModule
import me.patrykanuszczyk.traitorbot.voicechatroles.VoicechatRole
import me.patrykanuszczyk.traitorbot.voicechatroles.VoicechatRolesEventListener
import me.patrykanuszczyk.traitorbot.voicechatroles.VoicechatRolesModule
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class TraitorBot(secretConfig: SecretConfig) {
    val database: Database = Database.connect(
        secretConfig.databaseAuth.url,
        driver = "com.mysql.jdbc.Driver",
        user = secretConfig.databaseAuth.user,
        password = secretConfig.databaseAuth.password
    )

    val discord: JDA
    val commandManager = CommandManager(this)
    private var _modules = mutableSetOf<BotModule>()
    val modules get() = _modules as Set<BotModule>

    init {
        val jdaBuilder = JDABuilder.createDefault(secretConfig.botToken)
        jdaBuilder.addEventListeners(
            commandManager,
            VoicechatRolesEventListener(this)
        )
        discord = jdaBuilder.build()
        _modules.add(AdminModule(this))
        _modules.add(VoicechatRolesModule(this))
        _modules.add(GuildPrefixModule(this))
    }

    fun hasGlobalPermission(user: User, permission: String): Boolean {
        return getGlobalPermissions(user.idLong).any { userPerm ->
            if (userPerm.endsWith('*'))
                permission.startsWith(userPerm.removeSuffix("*"), true)
            else
                permission.equals(userPerm, true)
        }
    }

    fun getGlobalPermissions(id: Long): List<String> {
        return transaction {
            GlobalPermission.find { GlobalPermissionsTable.user eq id }.toList().map { it.permission }
        }
    }

    fun parseCommand(message: Message): Pair<String, String>? {
        val content = message.contentRaw

        val mentionA = "<@${discord.selfUser.id}>"
        val mentionB = "<@!${discord.selfUser.id}>"
        val prefix = getPrefixFor(message.guild)

        var fullCommand: String? = null

        if(content.startsWith(mentionA))
            fullCommand = content.substring(mentionA.length)
        else if(content.startsWith(mentionB))
            fullCommand = content.substring(mentionB.length)
        else if(prefix != null && content.startsWith(prefix))
            fullCommand = content.substring(prefix.length)

        if(fullCommand == null) return null

        val split = fullCommand.trim().split(' ', limit = 2)

        return split[0] to if(split.size > 1) split[1].trimStart() else ""
    }

    /**
     * Gets guild prefix or null, if none is set.
     */
    fun getPrefixFor(guild: Guild): String? {
        return transaction {
            GuildPrefix.findById(guild.idLong)
        }?.prefix
    }
}