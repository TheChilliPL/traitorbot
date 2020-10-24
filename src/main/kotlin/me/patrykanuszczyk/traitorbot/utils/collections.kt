package me.patrykanuszczyk.traitorbot.utils

fun List<Int>.normalize(max: Int): List<Int> {
    if(isEmpty()) return emptyList()
    val initMax = this.max()!!
    if(initMax == 0) return List(size) {0}
    return map { it * max / initMax }
}

fun <E> MutableCollection<E>.addAll(vararg elements: E): Boolean {
    return this.addAll(elements)
}

fun <E> MutableCollection<E>.addUnique(element: E) {
    if(!contains(element)) add(element)
}