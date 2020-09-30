package me.patrykanuszczyk.traitorbot

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import java.io.FileReader
import java.net.InetAddress

fun main() {
//    val address = InetAddress.getByName("patrykanuszczyk.me");
//    println(address.isReachable(5000))
    val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
        .create()

    val reader = JsonReader(FileReader("secret.json"))
    val secretConfig = gson.fromJson<SecretConfig>(reader, SecretConfig::class.java)

//    println("""
//        Token ${secretConfig.botToken}
//        ${secretConfig.databaseAuth.user}@${secretConfig.databaseAuth.url}
//        Password ${secretConfig.databaseAuth.password}
//    """.trimIndent())

    TraitorBot(
        secretConfig
    )
}