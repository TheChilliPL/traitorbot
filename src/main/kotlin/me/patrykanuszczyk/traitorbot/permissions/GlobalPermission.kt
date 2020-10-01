package me.patrykanuszczyk.traitorbot.permissions

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class GlobalPermission(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<GlobalPermission>(GlobalPermissionsTable)
    var user by GlobalPermissionsTable.user
    var permission by GlobalPermissionsTable.permission
}