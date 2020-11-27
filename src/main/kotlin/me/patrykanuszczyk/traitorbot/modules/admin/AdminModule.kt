package me.patrykanuszczyk.traitorbot.modules.admin

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.*
import me.patrykanuszczyk.traitorbot.commands.arguments.DiscordCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.modules.BotModule
import me.patrykanuszczyk.traitorbot.utils.Result
import me.patrykanuszczyk.traitorbot.utils.escapeFormatting
import me.patrykanuszczyk.traitorbot.utils.escapeFormattingInCode
import me.patrykanuszczyk.traitorbot.utils.escapeMentions
import kotlin.system.exitProcess

class AdminModule(bot: TraitorBot) : BotModule(bot) {
    val stopCommand = Command("stop") {
        if(!it.hasGlobalPermission("admin.bot_stop"))
            return@Command bot.commandManager.sendNoPermissionMessage(it)

        it.reply(":thumbsup:")
        exitProcess(0)
    }.andRegister(bot)

    val throwCommand = Command("throw") {
        if(!it.hasGlobalPermission("admin.throw"))
            return@Command bot.commandManager.sendNoPermissionMessage(it)

        throw Exception("Test exception thrown by the administrator")
    }.andRegister(bot)

    val paramTest = Command("param_test") { arguments ->
        var a = 0
        var b = 0
        var c = 0
        var d = 0
        var e = 0
        var text = "(empty)"
        val left = parseParameters(
            arguments.parameters,
            Parameter("a", "first") { a++ },
            Parameter("b", "second") { b++ },
            Parameter("c", "third") { c++ },
            Parameter("d", "fourth") { d++ },
            Parameter("e", "fifth") { e++ },
            Parameter("text", getInput = true) { text = it!! }
        )
        if (!left.successful) {
            arguments.reply((left as Result.Failure).value)
            return@Command
        }
        arguments.reply(
            """
            a $a
            b $b
            c $c
            d $d
            e $e
            text $text
            more: ${(left as Result.Success).value}
            """.trimIndent()
        )
    }.withAliases("ptest").andRegister(bot)

    val escTest = Command("esc_test") { args ->
        if(args !is DiscordCommandInvokeArguments || !args.isFromGuild)
            return@Command args.reply("Musisz być na serwerze.")
        args.reply(args.parameters.escapeFormatting().escapeMentions(args.guild!!))
    }.andRegister(bot)

    val codeTest = Command("code_test") { args ->
        if(args !is DiscordCommandInvokeArguments || !args.isFromGuild)
            return@Command args.reply("Musisz być na serwerze.")
        args.reply(
            "```\n"
                + args.parameters.escapeFormattingInCode()
                .escapeMentions(args.guild!!) + "\n```"
        )
    }.andRegister(bot)
}