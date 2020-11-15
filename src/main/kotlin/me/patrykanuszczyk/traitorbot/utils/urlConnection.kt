package me.patrykanuszczyk.traitorbot.utils

import java.io.InputStream
import java.net.HttpURLConnection

val HttpURLConnection.isError: Boolean
    get() {
        return responseCode >= 300
    }

val HttpURLConnection.inputOrErrorStream: InputStream
    get() {
        return if (isError) errorStream else inputStream
    }