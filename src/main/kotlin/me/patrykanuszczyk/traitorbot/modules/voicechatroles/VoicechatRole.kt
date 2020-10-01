package me.patrykanuszczyk.traitorbot.modules.voicechatroles

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class VoicechatRole(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<VoicechatRole>(VoicechatRolesTable)

    var guild by VoicechatRolesTable.guild
    var channel by VoicechatRolesTable.channel
    var role by VoicechatRolesTable.role

    override fun toString(): String {
        return "$channel@$guild -> $role"
    }
}