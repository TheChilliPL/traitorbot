package me.patrykanuszczyk.traitorbot.modules.alias

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.CommandExecutionArguments
import me.patrykanuszczyk.traitorbot.commands.CommandExecutor
import me.patrykanuszczyk.traitorbot.modules.BotModule
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteIgnoreWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class AliasesModule(bot: TraitorBot) : BotModule(bot), CommandExecutor {
    val aliasCommand = Command(
        "alias",
        setOf("a"),
        "",
        this
    )

    init {
        bot.commandManager.registerCommand(aliasCommand)
    }

    override fun executeCommand(args: CommandExecutionArguments) {
        val split = args.arguments.split(" ", limit = 2)
        if(split.size < 2) return
        when(split[0]) {
            "add" -> {

            }
        }
    }

    fun addAlias(message: Message, alias: String, command: String) {
        transaction {
            AliasesTable.insert {
                it[guild] = message.guild.idLong
                it[this.alias] = alias
                it[this.command] = command
                it[this.message] = message.idLong
            }
        }
    }

    fun removeAlias(guild: Guild, alias: String) {
        transaction {
            AliasesTable.deleteIgnoreWhere {
                (AliasesTable.alias eq alias) and (AliasesTable.guild eq guild.idLong)
            }
        }
    }

    fun getAlias(guild: Guild, alias: String): String? {
        return transaction {
            AliasesTable.select {
                AliasesTable.guild.eq(guild.idLong) and AliasesTable.alias.eq(alias)
            }.limit(1).firstOrNull()?.get(AliasesTable.command)
        }
    }
}