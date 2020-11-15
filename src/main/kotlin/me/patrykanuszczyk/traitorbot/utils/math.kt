package me.patrykanuszczyk.traitorbot.utils

import kotlin.math.pow
import kotlin.math.roundToLong

fun Double.round(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals)

    return (this * multiplier).roundToLong() / multiplier
}

val niceFormatUnits = listOf("", "k", "M")
fun Number.niceFormat(
    units: List<String> = niceFormatUnits,
    precision: Int = 1
): String {
    var num = toDouble()
    var unitId = 0
    while(num > 1000 && unitId < units.size - 1) {
        num /= 1000
        unitId++
    }
    num = num.round(precision)
    return "$num${units[unitId]}"
}