package me.patrykanuszczyk.traitorbot.utils

val digits = ('0'..'9').toList()
val bigLetters = ('A'..'Z').toList()
val smallLetters = ('a'..'z').toList()
val alphanumerics = digits + bigLetters + smallLetters

fun getRandomString(characters: Collection<Char>, length: Int): String {
    val sb = StringBuilder(length)
    for(i in 0 until length)
        sb.append(characters.random())
    return sb.toString()
}