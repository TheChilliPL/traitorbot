package me.patrykanuszczyk.traitorbot.modules.prefix

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class GuildPrefix(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<GuildPrefix>(GuildPrefixTable)

    var prefix by GuildPrefixTable.prefix
}