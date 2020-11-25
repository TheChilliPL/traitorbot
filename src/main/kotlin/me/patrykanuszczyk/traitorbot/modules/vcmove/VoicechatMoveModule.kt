package me.patrykanuszczyk.traitorbot.modules.vcmove

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.arguments.DiscordCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.commands.parseParameters
import me.patrykanuszczyk.traitorbot.modules.BotModule
import me.patrykanuszczyk.traitorbot.utils.moveTo
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.VoiceChannel

class VoicechatMoveModule(bot: TraitorBot) : BotModule(bot) {
    val vcmoveCommand = Command("vcmove") {
        if (it !is DiscordCommandInvokeArguments || !it.isFromGuild) {
            return@Command it.reply("Tej komendy można użyć tylko na serwerze!")
        }

        if (!it.invokerMember!!.hasPermission(Permission.VOICE_MOVE_OTHERS))
            return@Command bot.commandManager.sendNoPermissionMessage(it)

        val parameters = parseParameters(it.parameters).fail { error ->
            return@Command it.reply(error)
        }

        if (parameters.size != 2)
            return@Command it.reply("Musisz podać dokładnie 2 parametry.")

        fun getVoiceChannel(string: String): VoiceChannel? {
            val id = string.removeSurrounding("<#", ">").toLongOrNull()

            return if (id != null)
                it.guild!!.getVoiceChannelById(id)
            else
                it.guild!!.voiceChannels.singleOrNull { vc ->
                    vc.name.contains(string)
                }
        }

        fun didntFindVoiceChannel() {
            it.reply("Nie znaleziono podanego kanału VC.")
        }

        val vcFrom = getVoiceChannel(parameters[0])
            ?: return@Command didntFindVoiceChannel()

        val vcTo = getVoiceChannel(parameters[1])
            ?: return@Command didntFindVoiceChannel()

        if (vcFrom == vcTo) {
            return@Command it.reply("Podałeś dwa razy ten sam kanał.")
        }

        if(vcFrom.members.isEmpty())
            return@Command it.reply("Kanał jest pusty.")

        val moved = vcFrom.members.map { member ->
            try {
                Triple(member, member.moveTo(vcTo).submit(), null)
            } catch (e: Exception) {
                Triple(member, null, e)
            }
        }.map { (member, future, e) ->
            if(e != null) return@map member to e
            try {
                future!!.join()
                return@map member to null
            } catch (exception: Exception) {
                return@map member to exception
            }
        }

        if (moved.all { m -> m.second == null })
            return@Command it.reply("Przesunięto wszystkich na kanale.\n")

        if (moved.none { m -> m.second == null })
            return@Command it.reply(
                "Nie można było nikogo przesunąć:\n" +
                    moved.joinToString("\n") { m ->
                        "- " + m.first.user.name + "#" + m.first.user.discriminator +
                            " → " + m.second!!.javaClass.simpleName
                    }
            )

        it.reply(
            "Przesunięto następujące osoby:\n" +
                moved.filter { m -> m.second == null }
                    .joinToString("\n") { m -> "- " + m.first.user.name + "#" + m.first.user.discriminator } +
                "\nJednak nie można było przesunąć:\n" +
                moved.filter { m -> m.second != null }.joinToString("\n") { m ->
                    "- " + m.first.user.name + "#" + m.first.user.discriminator +
                        " → " + m.second!!.javaClass
                }
        )
    }.andRegister(bot)
}