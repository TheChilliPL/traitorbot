package me.patrykanuszczyk.traitorbot

class SecretConfig(
    val botToken: String,
    val databaseAuth: DatabaseAuth
) {
    class DatabaseAuth (
        val url: String,
        val user: String,
        val password: String
    )
}