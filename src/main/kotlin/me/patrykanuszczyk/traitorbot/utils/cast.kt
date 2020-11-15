package me.patrykanuszczyk.traitorbot.utils

import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Any.asType() : T {
    return this as T
}

inline infix fun <T1 : Any, reified T2 : T1> T1.maybeAs(@Suppress("UNUSED_PARAMETER") clazz: KClass<T2>): T2? {
    return if(this is T2) this
    else null
}