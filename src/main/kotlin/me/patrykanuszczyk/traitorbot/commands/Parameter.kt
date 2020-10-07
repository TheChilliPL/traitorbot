package me.patrykanuszczyk.traitorbot.commands

class Parameter (
    vararg names: String,
    val getInput: Boolean = false,
    val run: (String?) -> Unit
) {
    val names = names.asList()
}