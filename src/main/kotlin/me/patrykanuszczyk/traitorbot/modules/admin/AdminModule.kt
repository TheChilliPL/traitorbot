package me.patrykanuszczyk.traitorbot.modules.admin

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.CommandExecutionArguments
import me.patrykanuszczyk.traitorbot.commands.CommandExecutor
import me.patrykanuszczyk.traitorbot.modules.BotModule
import net.dv8tion.jda.api.OnlineStatus

class AdminModule(bot: TraitorBot) : BotModule(bot), CommandExecutor {
    val stopCommand = Command(
        "stop",
        emptySet(),
        "Stops the bot",
        this
    )

    init {
        bot.commandManager.registerCommand(stopCommand)
    }

    override fun executeCommand(args: CommandExecutionArguments) {
        when (args.command) {
            stopCommand -> {
                if (!bot.hasGlobalPermission(args.user, "admin.bot_stop")) {
                    bot.commandManager.sendNoPermissionMessage(args)
                    return
                }

                args.channel.sendMessage(":ok_hand: Już się robi, ${args.user.asMention}!").submit()
                bot.discord.presence.setStatus(OnlineStatus.OFFLINE)
                bot.discord.shutdown()
                return
            }
        }
    }
}