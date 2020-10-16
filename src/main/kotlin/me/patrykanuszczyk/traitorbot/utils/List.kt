package me.patrykanuszczyk.traitorbot.utils

fun List<Int>.normalize(max: Int): List<Int> {
    if(isEmpty()) return emptyList()
    val initMax = this.max()!!
    if(initMax == 0) return List(size) {0}
    return map { it * max / initMax }
}

/**
 * Returns the single element matching the given [predicate], or `null` if there is no or more than one matching element.
 */
inline fun <T> Iterable<T>.singleOrNull(predicate: (T) -> Boolean): T? {
    var single: T? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    return single
}