package me.patrykanuszczyk.traitorbot.permissions

import org.jetbrains.exposed.dao.id.IntIdTable

object GlobalPermissionsTable : IntIdTable("global_permissions") {
    val user = long("user")
    val permission = text("permission")
}