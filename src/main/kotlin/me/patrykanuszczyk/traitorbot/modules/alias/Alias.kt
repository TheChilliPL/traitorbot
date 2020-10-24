package me.patrykanuszczyk.traitorbot.modules.alias

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Alias(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Alias>(AliasesTable)

    var guild by AliasesTable.guild
    var alias by AliasesTable.alias
    var command by AliasesTable.command
    var message by AliasesTable.message
}