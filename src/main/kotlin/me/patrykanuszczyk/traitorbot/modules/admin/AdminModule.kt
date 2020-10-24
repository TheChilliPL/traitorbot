package me.patrykanuszczyk.traitorbot.modules.admin

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.*
import me.patrykanuszczyk.traitorbot.commands.arguments.CommandInvokeArguments
import me.patrykanuszczyk.traitorbot.commands.arguments.DiscordCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.modules.BotModule
import me.patrykanuszczyk.traitorbot.utils.Result
import java.time.Duration
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

    val pingCommand = Command("ping") {
        if (it !is DiscordCommandInvokeArguments)
            return@Command it.reply("Ta komenda musi być wykonana na Discordzie")

        val msg = it.channel
            .sendMessage(":timer: Testowanie pingu...").complete()
            .editMessage(":timer: Testowanie pingu...").complete()

        val ping = Duration.between(msg.timeCreated, msg.timeEdited!!).toNanos()
            .toFloat() / 1e6

        msg.editMessage("Ping wynosi $ping ms!").complete()
    }.andRegister(bot)
}