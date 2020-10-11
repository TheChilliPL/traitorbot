package me.patrykanuszczyk.traitorbot.modules.alias

import org.jetbrains.exposed.dao.id.IntIdTable

object AliasesTable : IntIdTable("command_aliases") {
    val guild = long("guild")
    val alias = text("alias")
    val command = text("command")
    val message = long("message")
}