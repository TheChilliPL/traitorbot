package me.patrykanuszczyk.traitorbot.prefix

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table

class GuildPrefix(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<GuildPrefix>(GuildPrefixTable)
    var prefix by GuildPrefixTable.prefix
}