package me.patrykanuszczyk.traitorbot

import me.patrykanuszczyk.traitorbot.utils.Result

class SecretConfig(
    val botToken: String?,
    val databaseAuth: DatabaseAuth?
) {
    class DatabaseAuth(
        val url: String?,
        val user: String?,
        val password: String?
    )

    fun verify(): Result<Unit, String> {
        return when {
            botToken == null ->
                Result.Failure("Bot token is missing at key \"bot_token\".")
            databaseAuth == null ->
                Result.Failure("Database authentication is missing at key \"database_auth\".")
            databaseAuth.url == null ->
                Result.Failure("Database URL is missing at key \"database_auth.url\".")
            databaseAuth.user == null ->
                Result.Failure("Database username is missing at key \"database_auth.user\".")
            databaseAuth.password == null ->
                Result.Failure("Database password is missing at key \"database_auth.password\".")
            else -> Result.Success(Unit)
        }
    }
}