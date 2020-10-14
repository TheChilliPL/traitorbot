package me.patrykanuszczyk.traitorbot.modules.admin

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.*
import me.patrykanuszczyk.traitorbot.modules.BotModule
import me.patrykanuszczyk.traitorbot.utils.Result
import net.dv8tion.jda.api.OnlineStatus
import kotlin.system.exitProcess

class AdminModule(bot: TraitorBot) : BotModule(bot), CommandExecutor {
    val stopCommand = Command(
        "stop",
        emptySet(),
        "Stops the bot",
        this
    )
    val parameterTestCommand = Command(
        "paramtest",
        setOf("ptest"),
        "Tests bash-like parameters",
        this
    )
    val throwCommand = Command(
        "throw",
        emptySet(),
        "Throws an exception",
        this
    )

    init {
        bot.commandManager.registerCommand(stopCommand)
        bot.commandManager.registerCommand(parameterTestCommand)
        bot.commandManager.registerCommand(throwCommand)
    }

    override fun executeCommand(args: CommandExecutionArguments) {
        when (args.command) {
            stopCommand -> {
                if (!bot.hasGlobalPermission(args.user, "admin.bot_stop")) {
                    bot.commandManager.sendNoPermissionMessage(args)
                    return
                }

                args.channel.sendMessage(":ok_hand: Już się robi, ${args.user.asMention}!").complete()
                exitProcess(0)
            }
            parameterTestCommand -> {
                var a = 0
                var b = 0
                var c = 0
                var d = 0
                var e = 0
                var text = "(empty)"
                val left = parseParameters(
                    args.arguments,
                    Parameter("a", "first") { a++ },
                    Parameter("b", "second") { b++ },
                    Parameter("c", "third") { c++ },
                    Parameter("d", "fourth") { d++ },
                    Parameter("e", "fifth") { e++ },
                    Parameter("text", getInput = true) { text = it!! }
                )
                if (!left.successful) {
                    args.channel.sendMessage((left as Result.Failure).value).submit()
                    return
                }
                args.channel.sendMessage(
                    """
                    a $a
                    b $b
                    c $c
                    d $d
                    e $e
                    text $text
                    more: ${(left as Result.Success).value}
                    """.trimIndent()
                ).queue()
            }
            throwCommand -> {
                if (!bot.hasGlobalPermission(args.user, "admin.throw")) {
                    bot.commandManager.sendNoPermissionMessage(args)
                    return
                }
                throw Exception("Exception was thrown by the administrator.")
            }
        }
    }
}