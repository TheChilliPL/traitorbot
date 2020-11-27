package me.patrykanuszczyk.traitorbot.utils

fun List<Int>.normalize(max: Int): List<Int> {
    if(isEmpty()) return emptyList()
    val initMax = this.maxOrNull()!!
    if(initMax == 0) return List(size) {0}
    return map { it * max / initMax }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <E> MutableCollection<E>.addAll(vararg elements: E): Boolean {
    return this.addAll(elements)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <E> MutableCollection<E>.addUnique(element: E) {
    if(!contains(element)) add(element)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Iterator<T>.nextOrNull(): T? {
    return if(hasNext()) next() else null
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> PeekableIterator<T>.peekOrNull(): T? {
    return if(hasNext()) peek() else null
}