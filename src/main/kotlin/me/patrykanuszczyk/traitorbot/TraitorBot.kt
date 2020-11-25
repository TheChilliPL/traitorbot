package me.patrykanuszczyk.traitorbot

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.patrykanuszczyk.traitorbot.commands.CommandManager
import me.patrykanuszczyk.traitorbot.modules.BotModule
import me.patrykanuszczyk.traitorbot.modules.admin.AdminModule
import me.patrykanuszczyk.traitorbot.modules.mee6levels.Mee6LevelsModule
import me.patrykanuszczyk.traitorbot.modules.ping.PingModule
import me.patrykanuszczyk.traitorbot.modules.prefix.GuildPrefixModule
import me.patrykanuszczyk.traitorbot.modules.reddit.SubredditLinkerModule
import me.patrykanuszczyk.traitorbot.modules.vcmove.VoicechatMoveModule
import me.patrykanuszczyk.traitorbot.modules.voicechatroles.VoicechatRolesModule
import me.patrykanuszczyk.traitorbot.modules.voting.VotingModule
import me.patrykanuszczyk.traitorbot.utils.Result
import me.patrykanuszczyk.traitorbot.utils.addAll
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import kotlin.system.exitProcess

class TraitorBot(secretConfig: SecretConfig): EventListener {
    val database = HikariDataSource(HikariConfig().apply {
        secretConfig.databaseAuth!!.also { auth ->
            jdbcUrl = auth.url + urlExtra
            username = auth.user
            password = auth.password
        }
    })

    val discord: JDA
    val commandManager = CommandManager(this)
    val modules get() = _modules as Set<BotModule>

    private var _modules = mutableSetOf<BotModule>()

    val logger: Logger = LogManager.getLogger(this::class.java)

    init {
        instance = this
        discord = JDABuilder.createDefault(secretConfig.botToken).addEventListeners(this).build()
        _modules.addAll(
            AdminModule(this),
            GuildPrefixModule(this),
            Mee6LevelsModule(this),
            PingModule(this),
            SubredditLinkerModule(this),
            VoicechatMoveModule(this),
            VoicechatRolesModule(this),
            VotingModule(this)
        )
    }

    override fun onEvent(event: GenericEvent) {
        if(event is MessageReceivedEvent) {
            if(!commandManager.onMessageReceived(event))
                _modules.none { it.onMessageReceived(event) }
        }
    }

    fun hasGlobalPermission(user: User, permission: String): Boolean {
        return getGlobalPermissions(user.idLong).any { userPerm ->
            if (userPerm.endsWith('*'))
                permission.startsWith(userPerm.removeSuffix("*"), true)
            else
                permission.equals(userPerm, true)
        }
    }

    private var globalPermissions: MutableMap<Long, List<String>>? = null

    init {
        val query = "SELECT user, permission FROM global_permissions"

        database.connection.use { conn ->
            val result = conn.createStatement().executeQuery(query)

            globalPermissions = generateSequence {
                if(!result.next()) null
                else result.getLong("user") to
                    result.getString("permission")
            }.groupBy({it.first}, {it.second}).toMutableMap()
        }
    }

    fun getGlobalPermissions(id: Long): List<String> {
        return globalPermissions?.get(id) ?: emptyList()
    }

    private var prefixes: MutableMap<Long, String>? = null

    init {
        val query = "SELECT id, prefix FROM guild_prefix"

        database.connection.use { conn ->
            val result = conn.createStatement().executeQuery(query)

            prefixes = generateSequence {
                if(!result.next()) null
                else result.getLong("id") to
                    result.getString("prefix")
            }.toMap().toMutableMap()
        }
    }

    /**
     * Gets guild prefix or `null`, if none is set or the [guild] is `null`.
     */
    fun getPrefixFor(guild: Guild?): String? {
        if (guild == null) return null
        return prefixes?.get(guild.idLong)
    }

    /**
     * Removes the prefix of a guild.
     * @return `true` if guild has had prefix before; `false` otherwise.
     */
    fun removePrefixFor(guild: Guild): Boolean {
        prefixes!!.remove(guild.idLong) ?: return false
        val query = "DELETE IGNORE FROM guild_prefix WHERE id = ?"
        database.connection.use { conn ->
            conn.prepareStatement(query).apply {
                setLong(1, guild.idLong)
            }.execute()
        }
        return true
    }

    /**
     * Sets the prefix of a guild.
     */
    fun setPrefixFor(guild: Guild, prefix: String) {
        prefixes!![guild.idLong] = prefix
        val query = """
            SET @id = ?, @prefix = ?;
            INSERT
                INTO guild_prefix (id, prefix)
                VALUES (@id, @prefix)
                ON DUPLICATE KEY UPDATE prefix = @prefix
        """
        database.connection.use { conn ->
            conn.prepareStatement(query).apply {
                setLong(1, guild.idLong)
                setString(2, prefix)
            }.execute()
        }
    }

    fun processShutdown() {
        logger.info("Shutting down...")
        database.close()
        discord.presence.setStatus(OnlineStatus.OFFLINE)
        discord.shutdown()
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            processShutdown()
        })
    }

    companion object {
        internal var instance: TraitorBot? = null

        private const val urlExtra =
            "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC"
    }
}

fun main() {
    @Suppress("unused")
    fun Unit.andExit(): Nothing {
        exitProcess(1)
    }

    val mainLogger = LogManager.getLogger("me.patrykanuszczyk.traitorbot.main")

    val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
        .create()

    val file = File("secret.json")

    if(!file.exists())
        mainLogger.fatal("Couldn't find file secret.json needed to connect to Discord and the database.").andExit()
    if(!file.isFile)
        mainLogger.fatal("The file secret.json is not a normal file.").andExit()
    if(!file.canRead())
        mainLogger.fatal("Can't read the file secret.json, probably because of missing permissions.").andExit()

    val reader = JsonReader(file.reader())
    val secretConfig = try {
        gson.fromJson<SecretConfig>(reader, SecretConfig::class.java)
    } catch(e : JsonSyntaxException) {
        mainLogger.fatal("A syntax error occurred trying to read secret.json.", e).andExit()
    }

    val verification = secretConfig.verify()

    if(verification is Result.Failure) {
        mainLogger.fatal(verification.value).andExit()
    }

    TraitorBot(
        secretConfig
    )
}