package me.patrykanuszczyk.traitorbot.commands

data class Command(
    val name: String,
    val aliases: Set<String>,
    val description: String,
    val executor: CommandExecutor
)