package me.patrykanuszczyk.traitorbot.utils

fun List<Int>.normalize(max: Int): List<Int> {
    if(isEmpty()) return emptyList()
    val initMax = this.max()!!
    if(initMax == 0) return List(size) {0}
    return map { it * max / initMax }
}