package me.patrykanuszczyk.traitorbot.utils

fun <E> MutableList<E>.addUnique(element: E) {
    if(!contains(element)) add(element)
}