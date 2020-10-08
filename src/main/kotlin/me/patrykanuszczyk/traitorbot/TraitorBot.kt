package me.patrykanuszczyk.traitorbot

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import me.patrykanuszczyk.traitorbot.commands.CommandManager
import me.patrykanuszczyk.traitorbot.modules.BotModule
import me.patrykanuszczyk.traitorbot.modules.admin.AdminModule
import me.patrykanuszczyk.traitorbot.modules.prefix.GuildPrefix
import me.patrykanuszczyk.traitorbot.modules.prefix.GuildPrefixModule
import me.patrykanuszczyk.traitorbot.modules.voicechatroles.VoicechatRolesModule
import me.patrykanuszczyk.traitorbot.modules.voting.VotingModule
import me.patrykanuszczyk.traitorbot.permissions.GlobalPermission
import me.patrykanuszczyk.traitorbot.permissions.GlobalPermissionsTable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.FileReader

class TraitorBot(secretConfig: SecretConfig) {
    val database: Database = Database.connect(
        secretConfig.databaseAuth.url,
        driver = "com.mysql.jdbc.Driver",
        user = secretConfig.databaseAuth.user,
        password = secretConfig.databaseAuth.password
    )

    val discord: JDA
    val commandManager = CommandManager(this)
    val modules get() = _modules as Set<BotModule>

    private var _modules = mutableSetOf<BotModule>()

    init {
        discord = JDABuilder.createDefault(secretConfig.botToken).addEventListeners(commandManager).build()
        _modules.add(AdminModule(this))
        _modules.add(VoicechatRolesModule(this))
        _modules.add(GuildPrefixModule(this))
        _modules.add(VotingModule(this))
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

    @Deprecated(
        "Moved to CommandManager and renamed to parseCommandMessage.",
        ReplaceWith("this.commandManager.parseCommandMessage(message)"), DeprecationLevel.ERROR
    )
    fun parseCommand(message: Message): Pair<String, String>? {
        return commandManager.parseCommandMessage(message)
    }

    /**
     * Gets guild prefix or null, if none is set.
     * If passed guild is null, returns null.
     */
    fun getPrefixFor(guild: Guild?): String? {
        if (guild == null) return null
        return transaction {
            GuildPrefix.findById(guild.idLong)
        }?.prefix
    }
}

fun main() {
    val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
        .create()

    val reader = JsonReader(FileReader("secret.json"))
    val secretConfig = gson.fromJson<SecretConfig>(reader, SecretConfig::class.java)

    TraitorBot(
        secretConfig
    )
}