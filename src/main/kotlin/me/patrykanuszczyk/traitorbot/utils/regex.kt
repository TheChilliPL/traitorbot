package me.patrykanuszczyk.traitorbot.utils

operator fun MatchResult.get(index: Int): String {
    return this.groupValues[index]
}