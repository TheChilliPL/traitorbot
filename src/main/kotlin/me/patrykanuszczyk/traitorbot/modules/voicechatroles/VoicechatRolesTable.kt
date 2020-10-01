package me.patrykanuszczyk.traitorbot.modules.voicechatroles

import org.jetbrains.exposed.dao.id.IntIdTable

object VoicechatRolesTable : IntIdTable("voicechat_roles") {
    val guild = long("guild")
    val channel = long("channel")
    val role = long("role")
}