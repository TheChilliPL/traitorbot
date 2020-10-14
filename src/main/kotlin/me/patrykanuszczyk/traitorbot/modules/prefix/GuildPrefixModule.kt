package me.patrykanuszczyk.traitorbot.modules.prefix

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.BranchCommand
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.arguments.DiscordCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.commands.arguments.MessageCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.modules.BotModule
import net.dv8tion.jda.api.Permission

class GuildPrefixModule(bot: TraitorBot): BotModule(bot) {
    val prefixCommand = BranchCommand(
        "prefix",
        Command("set") {
            if(it !is MessageCommandInvokeArguments)
                return@Command it.reply("Ta komenda musi być wywołana na Discordzie")

            if(!it.isFromGuild)
                return@Command it.reply("Ta komenda musi być wykonana na serwerze")

            it.invokerMember!!.hasPermission(Permission.MANAGE_SERVER)
        }.withAliases("add")
    ).andRegister(bot)
}

//import me.patrykanuszczyk.traitorbot.TraitorBot
//import me.patrykanuszczyk.traitorbot.commands.Command
//import me.patrykanuszczyk.traitorbot.commands.arguments.CommandInvokeArguments
//import me.patrykanuszczyk.traitorbot.exposedutils.insertOrUpdate
//import me.patrykanuszczyk.traitorbot.modules.BotModule
//import net.dv8tion.jda.api.Permission
//import org.jetbrains.exposed.sql.deleteIgnoreWhere
//import org.jetbrains.exposed.sql.transactions.transaction
//
//class GuildPrefixModule(bot: TraitorBot) : BotModule(bot), CommandExecutor {
//    val prefixCommand = Command(
//        "prefix",
//        setOf("prf"),
//        "Sprawdza lub zmienia prefix bota na danym serwerze",
//        this
//    )
//
//    init {
//        bot.commandManager.registerCommand(prefixCommand)
//    }
//
//    override fun executeCommand(args: CommandInvokeArguments) {
//        if (args.command != prefixCommand) return
//
//        val cmdArgs = args.arguments.split(" ", limit = 2)
//
//        when (cmdArgs.firstOrNull()) {
//            "set" -> {
//                if (args.member?.hasPermission(Permission.MANAGE_SERVER) == false) {
//                    bot.commandManager.sendNoPermissionMessage(args)
//                } else if (cmdArgs.size <= 1 || cmdArgs[1].isEmpty()) {
//                    args.channel.sendMessage("${args.user.asMention}, prefix nie może być pusty.").submit()
//                } else {
//                    transaction {
//                        GuildPrefixTable.insertOrUpdate(
//                            GuildPrefixTable.prefix
//                        ) {
//                            it[id] = args.guild.idLong
//                            it[prefix] = cmdArgs[1]
//                        }
//                    }
//                    args.channel.sendMessage("${args.user.asMention}, prefix serwera zmieniono!")
//                        .submit()
//                }
//            }
//            in setOf("remove", "delete", "del") -> {
//                if (args.member?.hasPermission(Permission.MANAGE_SERVER) == false) {
//                    bot.commandManager.sendNoPermissionMessage(args)
//                } else {
//                    transaction {
//                        GuildPrefixTable.deleteIgnoreWhere {
//                            GuildPrefixTable.id eq args.guild.idLong
//                        }
//                    }
//                    args.channel.sendMessage("${args.user.asMention}, prefix został usunięty!")
//                        .submit()
//                }
//            }
//            else -> when (val prefix = bot.getPrefixFor(args.guild)) {
//                null -> {
//                    args.channel.sendMessage(
//                        "${args.user.asMention}, obecnie nie ma ustawionego prefixu na tym serwerze."
//                    ).submit()
//                }
//                else -> {
//                    args.channel.sendMessage(
//                        "${args.user.asMention}, obecny prefix na tym serwerze to `${prefix}`."
//                    ).submit()
//                }
//            }
//        }
//    }
//}