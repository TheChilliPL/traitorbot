package me.patrykanuszczyk.traitorbot.voicechatroles

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object VoicechatRolesTable : IntIdTable("voicechat_roles") {
    val guild = long("guild")
    val channel = long("channel")
    val role = long("role")
}