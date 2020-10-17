package me.patrykanuszczyk.traitorbot.modules.voting

import net.dv8tion.jda.api.entities.Message

class Voting (
    val name: String,
    val title: String,
    val answers: List<String>,
    val message: Message
) {
    var results: List<MutableList<Long>> = List(answers.size) { mutableListOf<Long>() }
}