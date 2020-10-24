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
import me.patrykanuszczyk.traitorbot.utils.normalize
import me.patrykanuszczyk.traitorbot.utils.reactionCode
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.SelfUser
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.internal.handle.MessageReactionBulkRemoveHandler
import org.jetbrains.annotations.Contract
import java.awt.Color
import kotlin.concurrent.thread

class VotingModule(bot: TraitorBot) : BotModule(bot), EventListener {
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

        if(parameters.failed)
            return@Command it.reply(parameters.failValue!!)

        val answerCount = parameters.successValue!!.size - 1

        if(answerCount < 2)
            return@Command it.reply("Musisz podać tytuł i co najmniej dwie odpowiedzi.")
        else if(answerCount > voteEmoji.size)
            return@Command it.reply("Maksymalna ilość odpowiedzi to ${voteEmoji.size}.")

        val title = parameters.successValue!!.first()
        val answers = parameters.successValue!!.drop(1)

        // Ensure guild voting map exists
        votings.computeIfAbsent(it.guild!!.idLong) { mutableMapOf() }

        // Random name generating
        if(name == null) {
            do {
                name = getRandomString(alphanumerics, 10)
            } while(name in votings[it.guild!!.idLong]!!)
        }

        val message = it.channel.sendMessage(":alarm_clock: Przygotowywanie głosowania…").complete()

        val voting = Voting(name!!, title, answers, message)
        votings[it.guild!!.idLong]!!.set(name!!, voting)
        votingMessageCache[message.idLong] = voting
        val uvmThread = thread {
            updateVotingMessage(voting)
        }
        val rvrThread = thread {
            refreshVotingReactions(voting)
        }
        uvmThread.join()
        rvrThread.join()
    }.withAliases("create", "add")

    val endCommand = Command("end") {
        if(it !is DiscordCommandInvokeArguments || !it.isFromGuild) {
            return@Command it.reply("Tej komendy można użyć tylko na serwerze!")
        }

        val parametersResult = parseParameters(it.parameters)

        if(parametersResult.failed)
            return@Command it.reply(parametersResult.failValue!!)

        val parameters = parametersResult.successValue!!

        if(parameters.size != 1) {
            return@Command it.reply("Musisz podać dokładnie jeden parametr — ID głosowania lub wiadomości.")
        }

        val voting = votingMessageCache[parameters.first().toLongOrNull()]
            ?: votings[it.guild!!.idLong]?.get(parameters.first())
            ?: return@Command it.reply("Nie znalaziono takiego głosowania.")

        closeVoting(voting)
        it.reply("Zamknięto podane głosowanie.")
    }

    val rootCommand = BranchCommand(
        "voting",
        newCommand, endCommand
    ).withAliases("vote").andRegister(bot)

    override fun onEvent(event: GenericEvent) {
        when(event) {
            is GenericMessageReactionEvent -> {
                val voting = votingMessageCache[event.messageIdLong] ?: return
                val user = event.retrieveUser().complete()
                if(user.isBot) {
                    if(user != bot.discord.selfUser || event !is MessageReactionRemoveEvent) return

                    event.retrieveMessage().complete().addReaction(event.reactionCode).queue()
                } else {
                    val index = getVoteIndex(voting, event.reactionCode) ?: return
                    when (event) {
                        is MessageReactionAddEvent -> vote(voting, user, index)
                        is MessageReactionRemoveEvent -> unvote(voting, user, index)
                    }
                }
            }
            is MessageReactionRemoveAllEvent -> {
                val voting = votingMessageCache[event.messageIdLong] ?: return
                voting.results = voting.answers.map { mutableSetOf<Long>() }
                updateVotingMessage(voting)
                refreshVotingReactions(voting)
            }
        }
    }

    init {
        bot.discord.addEventListener(this)
    }

    var votings = mutableMapOf<Long, MutableMap<String, Voting>>()
    var votingMessageCache = mutableMapOf<Long, Voting>()
    val voteEmoji = listOf("1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣")

    /**
     * Gets vote index for the specified voting and emoji.
     */
    fun getVoteIndex(voting: Voting, emoji: String): Int? {
        return voteEmoji.indexOf(emoji).takeIf { it in voting.answers.indices }
    }

    fun vote(voting: Voting, user: User, index: Int) {
        voting.results[index].add(user.idLong)
        updateVotingMessage(voting)
    }

    fun unvote(voting: Voting, user: User, index: Int) {
        voting.results[index].remove(user.idLong)
        updateVotingMessage(voting)
    }

    fun updateVotingMessage(voting: Voting, closed: Boolean = false) {
        voting.message.editMessage(
            EmbedBuilder()
                .setTitle(voting.title)
                .apply {
                    for((answer, votes) in voting.answers zip voting.results.map { it.size }.normalize(20)) {
                        addField(answer, "`["+"#".repeat(votes).padEnd(20)+"]`", false)
                    }
                }
                .setColor(if (closed) Color.GREEN else Color.GRAY)
                .setFooter("ID: " + voting.name + if(closed) " (zamknięte)" else "")
                .build()
        ).override(true).complete()
    }

    /**
     * Adds voting reactions to the specified voting.
     * It's synchronous and adds them in order.
     */
    fun refreshVotingReactions(voting: Voting) {
        for (index in voting.answers.indices) {
            voting.message.addReaction(voteEmoji[index]).complete()
        }
    }

    fun closeVoting(voting: Voting) {
        votings[voting.message.guild.idLong]?.remove(voting.name)
        votingMessageCache.remove(voting.message.idLong)
        updateVotingMessage(voting, true)
    }
}