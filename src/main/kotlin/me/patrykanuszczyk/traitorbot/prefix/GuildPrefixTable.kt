package me.patrykanuszczyk.traitorbot.prefix

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table

object GuildPrefixTable : LongIdTable("guild_prefix") {
    //val id = long("id").uniqueIndex()
    val prefix = text("prefix")
}