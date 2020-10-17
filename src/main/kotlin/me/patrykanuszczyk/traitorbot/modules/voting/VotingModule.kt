package me.patrykanuszczyk.traitorbot.modules.voting

import me.patrykanuszczyk.traitorbot.TraitorBot
import me.patrykanuszczyk.traitorbot.commands.BranchCommand
import me.patrykanuszczyk.traitorbot.commands.Command
import me.patrykanuszczyk.traitorbot.commands.Parameter
import me.patrykanuszczyk.traitorbot.commands.arguments.CommandInvokeArguments
import me.patrykanuszczyk.traitorbot.commands.arguments.DiscordCommandInvokeArguments
import me.patrykanuszczyk.traitorbot.commands.parseParameters
import me.patrykanuszczyk.traitorbot.modules.BotModule
import me.patrykanuszczyk.traitorbot.utils.alphanumerics
import me.patrykanuszczyk.traitorbot.utils.getRandomString
import org.jetbrains.annotations.Contract

class VotingModule(bot: TraitorBot) : BotModule(bot) {

    val newCommand = Command("new") {
        if(it !is DiscordCommandInvokeArguments || !it.isFromGuild) {
            return@Command it.reply("Tej komendy można użyć tylko na serwerze!")
        }

        var name: String? = null
        val parameters = try {
            parseParameters(
                it.parameters,
                Parameter("n", "name", getInput = true) { name = it }//,
//                Parameter("t", "timeout", getInput = true) { throw UnsupportedOperationException() },
//                Parameter("a", "anonymous") { throw UnsupportedOperationException() },
//                Parameter("d", "no-duplicates") { throw UnsupportedOperationException() },
//                Parameter("m", "min-votes", getInput = true) { throw UnsupportedOperationException() },
//                Parameter("w", "show-winners") { throw UnsupportedOperationException() },
//                Parameter("W", "unique-winner") { throw UnsupportedOperationException() },
//                Parameter("r", "needs-role") { throw UnsupportedOperationException() },
//                Parameter("no-removing") { throw UnsupportedOperationException() }
            )
        } catch (e: UnsupportedOperationException) {
            return@Command it.reply("Jeden z wykorzystanych parametrów nie jest obecnie obsługiwany.")
        }

        // Ensure guild voting map exists
        votings.computeIfAbsent(it.guild!!.idLong) { mutableMapOf() }

        // Random name generating
        if(name == null) {
            do {
                name = getRandomString(alphanumerics, 10)
            } while(name in votings[it.guild!!.idLong]!!)
        }

        //val voting = Voting(name!!, )
    }.withAliases("create", "add")

    val rootCommand = BranchCommand(
        "voting",
        newCommand
    ).andRegister(bot)

    var votings = mutableMapOf<Long, MutableMap<String, Voting>>()
    val voteEmoji = listOf("1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣")
}
//import me.patrykanuszczyk.traitorbot.TraitorBot
//import me.patrykanuszczyk.traitorbot.commands.*
//import me.patrykanuszczyk.traitorbot.commands.arguments.CommandInvokeArguments
//import me.patrykanuszczyk.traitorbot.modules.BotModule
//import me.patrykanuszczyk.traitorbot.utils.addUnique
//import me.patrykanuszczyk.traitorbot.utils.normalize
//import net.dv8tion.jda.api.EmbedBuilder
//import net.dv8tion.jda.api.entities.User
//import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
//import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
//import net.dv8tion.jda.api.hooks.ListenerAdapter
//import net.dv8tion.jda.api.requests.restaction.MessageAction
//import java.awt.Color
//
//class VotingModule(bot: TraitorBot) : BotModule(bot), CommandExecutor {
//    val votingCommand = Command(
//        "voting",
//        setOf("vote"),
//        "Allows to set up a voting.",
//        this
//    )
//    val voteEmoji = listOf("1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣")
//    val listener = Listener(this)
//
//    init {
//        bot.commandManager.registerCommand(votingCommand)
//        bot.discord.addEventListener(listener)
//    }
//
//    var votings = mutableMapOf<Long, Voting>()
//
//    override fun executeCommand(args: CommandInvokeArguments) {
//        val split = args.arguments.split(' ', limit = 2)
//        val command = split[0]
//        val arguments = split.getOrElse(1) {""}
//        when(command.toLowerCase()) {
//            "new" -> {
//                val message = args.channel.sendMessage(":timer:").submit().join()
//
//                var name: String? = null
//                val parameters = try {
//                    parseParameters(
//                        arguments,
//                        Parameter("n", "name", getInput = true) { name = it }//,
////                        Parameter("t", "timeout", getInput = true) { throw UnsupportedOperationException() },
////                        Parameter("a", "anonymous") { throw UnsupportedOperationException() },
////                        Parameter("d", "no-duplicates") { throw UnsupportedOperationException() },
////                        Parameter("m", "min-votes", getInput = true) { throw UnsupportedOperationException() },
////                        Parameter("w", "show-winners") { throw UnsupportedOperationException() },
////                        Parameter("W", "unique-winner") { throw UnsupportedOperationException() },
////                        Parameter("r", "needs-role") { throw UnsupportedOperationException() },
////                        Parameter("no-removing") { throw UnsupportedOperationException() }
//                    )
//                } catch (e: UnsupportedOperationException) {
//                    message.editMessage("Jeden z wykorzystanych parametrów nie jest obecnie obsługiwany.")
//                        .queue()
//                    return
//                }
//
//                if(parameters.failed) {
//                    message.editMessage(parameters.failValue!!).queue()
//                    return
//                }
//
//                val parametersResult = parameters.successValue!!
//                if (parametersResult.size < 3) {
//                    message.editMessage("Musisz podać co najmniej tytuł i dwie odpowiedzi.").queue()
//                    return
//                }
//                val title = parametersResult.first()
//                val answers = parametersResult.drop(1)
//                if(answers.size > voteEmoji.size) {
//                    message.editMessage("Za dużo odpowiedzi. Maksymalna ilość: ${voteEmoji.size}.").queue()
//                    return
//                }
//
//                val voting = Voting(name, title, answers, message)
//
//                votings[message.idLong] = voting
//                updateVotingMessage(voting).queue()
//
//                Thread {
//                    for (i in answers.indices) {
//                        message.addReaction(voteEmoji[i]).complete()
//                    }
//                }.start()
//            }
//            "end" -> {
//                val messageId = arguments.toLongOrNull()
//                if(messageId != null) {
//                    val message = votings[messageId]
//                    if(message != null) {
//                        votings.remove(messageId)
//                        closeVotingMessage(message).queue {
//                            args.channel.sendMessage("Zamknięto głosowanie.").queue()
//                        }
//                        return
//                    }
//                }
//                val voting = votings.entries.firstOrNull { it.value.name.equals(arguments, true) }
//                if(voting == null) {
//                    args.channel.sendMessage("Nie znaleziono głosowania o ID lub nazwie `$arguments`!").queue()
//                    return
//                }
//                votings.remove(voting.key)
//                closeVotingMessage(voting.value).queue {
//                    args.channel.sendMessage("Zamknięto głosowanie.").queue()
//                }
//            }
//            else -> args.channel.sendMessage("Poprawne podkomendy: new, end.").queue()
//        }
//    }
//
//    fun updateVotingMessage(voting: Voting, ended: Boolean = false): MessageAction {
//        val normalizedResults = voting.results.map { it.size }.normalize(20)
//        return voting.message.editMessage(
//            EmbedBuilder()
//                .setTitle(voting.title)
//                .setDescription("Zagłosowało osób: " + voting.results.flatten().distinct().size)
//                .apply {
//                    for((i, answer) in voting.answers.withIndex()) {
//                        addField(
//                            voteEmoji[i] + " **" + answer + "**",
//                            "`[" + "#".repeat(normalizedResults[i]).padEnd(20)
//                                + "]` ("+voting.results[i].size + ")",
//                            false
//                        )
//                    }
//                }
//                .setColor(if(ended) Color.GREEN else Color.CYAN)
//                .apply { if(voting.name != null) setFooter("ID: "+voting.name) }
//                .build()
//        ).override(true)
//    }
//
//    fun closeVotingMessage(voting: Voting): MessageAction {
//        return updateVotingMessage(voting, true)
//    }
//
//    fun addVote(voting: Voting, answer: Int, user: User) {
//        voting.results[answer].addUnique(user.idLong)
//        updateVotingMessage(voting).queue()
//    }
//
//    fun removeVote(voting: Voting, answer: Int, user: User) {
//        voting.results[answer].remove(user.idLong)
//        updateVotingMessage(voting).queue()
//    }
//
//    class Listener(val module: VotingModule) : ListenerAdapter() {
//        override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
//            if (event.retrieveUser().complete().isBot) return
//
//            val voting = module.votings[event.messageIdLong] ?: return
//            val answer = module.voteEmoji.indexOf(event.reactionEmote.emoji)
//            if(answer < 0) return
//            module.addVote(voting, answer, event.retrieveUser().complete())
//        }
//
//        override fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
//            if (event.retrieveUser().complete().isBot) return
//
//            val voting = module.votings[event.messageIdLong] ?: return
//            val answer = module.voteEmoji.indexOf(event.reactionEmote.emoji)
//            if(answer < 0) return
//            module.removeVote(voting, answer, event.retrieveUser().complete())
//        }
//    }
//}