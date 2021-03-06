package me.patrykanuszczyk.traitorbot.modules.prefix

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.BranchCommand
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.arguments.MessageCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.modules.BotModule
import net.dv8tion.jda.api.Permission

class GuildPrefixModule(bot: TraitorBot): BotModule(bot) {
    val prefixCommand = BranchCommand(
        "prefix",

        Command("set") { args ->
            if (args !is MessageCommandInvokeArguments || !args.isFromGuild)
                return@Command args.reply("Ta komenda musi być wykonana na serwerze")

            if (!args.invokerMember!!.hasPermission(Permission.MANAGE_SERVER))
                return@Command bot.commandManager.sendNoPermissionMessage(args)

            bot.setPrefixFor(args.guild!!, args.parameters)

            args.reply("Zmieniono prefix serwera na: `${args.parameters}`")
        }.withAliases("add"),

        Command("remove") { args ->
            if (args !is MessageCommandInvokeArguments || !args.isFromGuild)
                return@Command args.reply("Ta komenda musi być wykonana na serwerze")

            if (!args.invokerMember!!.hasPermission(Permission.MANAGE_SERVER))
                return@Command bot.commandManager.sendNoPermissionMessage(args)

            bot.removePrefixFor(args.guild!!)

            args.reply("Usunięto prefix serwera.")
        }.withAliases("del", "delete"),

        defaultBranch = Command("get") { args ->
            if (args !is MessageCommandInvokeArguments)
                return@Command args.reply("Ta komenda musi być wywołana na Discordzie")

            if (!args.isFromGuild)
                return@Command args.reply("Ta komenda musi być wykonana na serwerze")

            val prefix = bot.getPrefixFor(args.guild)

            args.reply(
                (if (prefix != null) "Obecny prefix to: `$prefix`." else "Nie ma ustawionego prefixu!") +
                    "\nAby zmienić prefix, użyj komendy `prefix set <prefix>` " +
                    "(potrzebujesz uprawnień zarządzania serwerem)."
            )
        }
    ).andRegister(bot)
}