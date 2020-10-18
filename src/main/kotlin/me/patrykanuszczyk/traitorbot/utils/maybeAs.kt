package me.patrykanuszczyk.traitorbot.utils

import kotlin.reflect.KClass

inline infix fun <T1 : Any, reified T2 : T1> T1.maybeAs(@Suppress("UNUSED_PARAMETER") clazz: KClass<T2>): T2? {
    return if(this is T2) this
    else null
}