package me.patrykanuszczyk.traitorbot.modules.prefix

import org.jetbrains.exposed.dao.id.LongIdTable

object GuildPrefixTable : LongIdTable("guild_prefix") {
    val prefix = text("prefix")
}