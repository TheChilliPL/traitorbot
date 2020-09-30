package me.patrykanuszczyk.traitorbot.commands

data class AliasIndexEntry (
    var commands: MutableSet<Command>,
    var isMainName: Boolean
)
