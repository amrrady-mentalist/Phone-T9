package com.arady.phone.net

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Talks to your API. Sending is fire-and-forget exactly like the web app: no URL means nothing is sent,
// and every error is swallowed.
object ApiClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Same JSON as the original: { "value": <word>, "sequence": <digits or event name> }
    fun send(url: String, sequence: String, value: String) {
        if (url.isBlank()) return
        scope.launch {
            try {
                postBlocking(url.trim(), sequence, value)
            } catch (_: Exception) {
            }
        }
    }

    private fun postBlocking(url: String, sequence: String, value: String) {
        val body = "{\"value\":" + JSONObject.quote(value) + ",\"sequence\":" + JSONObject.quote(sequence) + "}"
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            conn.responseCode // forces the request to complete
            try {
                (if (conn.responseCode >= 400) conn.errorStream else conn.inputStream)?.use { it.readBytes() }
            } catch (_: Exception) {
            }
        } finally {
            conn.disconnect()
        }
    }

    // Receiving: plain GET that returns the response body, or null on any failure.
    // Not wired into the UI yet - call it from the view model when you know what the API should return.
    suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        try {
            val conn = URL(url.trim()).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }
}
