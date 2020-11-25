package me.patrykanuszczyk.traitorbot.modules.ping

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.Parameter
import me.patrykanuszczyk.traitorbot.commands.arguments.DiscordCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.commands.arguments.MessageCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.commands.parseParameters
import me.patrykanuszczyk.traitorbot.modules.BotModule
import java.time.Duration
import java.time.Instant

class PingModule(bot: TraitorBot) : BotModule(bot) {
    val pingCommand = Command("ping") { args ->
        if (args !is DiscordCommandInvokeArguments)
            return@Command args.reply("Ta komenda musi być wykonana na Discordzie!")

        var amount: Int? = 1
        var full = false

        parseParameters(
            args.parameters,
            Parameter("a", "amount", getInput = true) { amount = it!!.toIntOrNull() },
            Parameter("f", "full") { full = true }
        ).fail { return@Command args.reply(it) }

        if(full) {
            if(args !is MessageCommandInvokeArguments)
                return@Command args.reply("Test pełnego pingu musi być przeprowadzony ręcznie komendą.")

            val msg = args.channel.sendMessage(":timer: Testowanie pełnego pingu...").complete()

            val ping = Duration.between(args.message.timeCreated, msg.timeCreated)
            val pingMs = ping.toNanos() / 1e6

            msg.editMessage("Pełny ping (między wysłaniem komendy a odpowiedzi) wynosi $pingMs ms.")
                .complete()

            return@Command
        }

        if (amount == null) return@Command args.reply("Podana ilość jest nieprawidłowa.")
        if (amount!! < 1) return@Command args.reply("Ilość musi wynosić co najmniej 1.")

        var msg = args.channel.sendMessage(":timer: Testowanie pingu 0/$amount...").complete()
        val timestamps = MutableList<Instant?>(amount!! + 1) { null }
        timestamps[0] = msg.timeCreated.toInstant()
        //.editMessage(":timer: Testowanie pingu...").complete()

        for (i in 1..amount!!) {
            msg = msg.editMessage(":timer: Testowanie pingu $i/$amount...").complete()
            timestamps[i] = msg.timeEdited!!.toInstant()
        }

        var ping = Duration.ZERO
        for (i in 0 until timestamps.size - 1) {
            ping += Duration.between(timestamps[i], timestamps[i + 1])
        }
        ping = ping.dividedBy(amount!!.toLong())

        val pingMs = ping.toNanos() / 1e6

        val pingStr = if (amount == 1) "Ping" else "Średni ping"

        val warning = if (amount!! <= 5) "" else
            "\n:warning: **Ilość pingów większa niż 5 zwraca nieprawidłowe wyniki przez ograniczenie szybkości " +
                "zapytań na Discordzie.**"

        msg.editMessage("$pingStr wynosi $pingMs ms!$warning").complete()
    }.andRegister(bot)
}