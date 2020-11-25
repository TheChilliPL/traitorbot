package me.patrykanuszczyk.traitorbot.modules

import me.patrykanuszczyk.traitorbot.TraitorBot
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class BotModule(val bot: TraitorBot) {
    open fun onMessageReceived(event: MessageReceivedEvent): Boolean {
        return false
    }
}