package me.patrykanuszczyk.traitorbot.modules.mee6levels

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.arguments.DiscordCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.modules.BotModule
import me.patrykanuszczyk.traitorbot.utils.asType
import me.patrykanuszczyk.traitorbot.utils.inputOrErrorStream
import me.patrykanuszczyk.traitorbot.utils.niceFormat
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class Mee6LevelsModule(bot: TraitorBot) : BotModule(bot) {
    val mee6LevelsCommand = Command("mee6levels") {
        if(it !is DiscordCommandInvokeArguments)
            return@Command it.reply("Musisz być na Discordzie aby użyć tej komendy!")

        val parameters = it.parameters.trim()

        val serverId = if(parameters.isNotEmpty()) {
            parameters.toLongOrNull() ?: return@Command it.reply(
                "Podane ID serwera jest nieprawidłowe."
            )
        } else {
            if(!it.isFromGuild) {
                return@Command it.reply("Musisz być na serwerze lub podać ID serwera.")
            }

            it.guild!!.idLong
        }

        val url = "https://mee6.xyz/api/plugins/levels/leaderboard/$serverId"

        val con = URL(url).openConnection().asType<HttpURLConnection>().apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }

        val reader = JsonReader(con.inputOrErrorStream.reader())

        val response = JsonParser.parseReader(reader).asJsonObject

        if(response.has("error"))
            return@Command it.reply("""
                Mee6 API returned an error (code ${con.responseCode}).
                ```
                ${response.getAsJsonObject("error")["message"].asString}
                ```
            """.trimIndent())

        val players = response.getAsJsonArray("players").map { p -> p.asJsonObject }

        val embed = EmbedBuilder()
            .setTitle("Najlepsi w Mee6")
            .apply {
                val writer = StringBuilder()
                for ((i, player) in players.withIndex().take(10)) {
                    val place = i+1
                    val mention = "<@${player["id"].asString}>"
                    val level = player["level"].asInt
                    var exp: Double
                    val progress = (player.getAsJsonArray("detailed_xp").run {
                        exp = get(2).asDouble
                        get(0).asFloat / get(1).asFloat
                    } * 20).roundToInt()
                    val progressBar = "#".repeat(progress).padEnd(20)
                    val expCount = exp.niceFormat()
                    val msgCount = player.get("message_count").asDouble.niceFormat()
                    writer.append("""
                        **$place. $mention** (__**$level**__ lv)
                        **`[$progressBar]`** *($msgCount msgs, $expCount xp)*
                        
                    """.trimIndent())
                }
                setDescription(writer)
            }
            .setColor(Color.GREEN)
            .build()

        it.channel.sendMessage(embed).complete()
    }.withAliases("m6l").andRegister(bot)
}
