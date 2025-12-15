package com.shakilpatel.boloindia.distributor.utils

import android.util.Log
import io.ktor.client.statement.*

suspend fun HttpResponse.logResponse(tag: String = "API_RESPONSE") {
    Log.d(tag, "╔═══════════════════════════════════════════════════════════")
    Log.d(tag, "║ Response for: ${request.url}")
    Log.d(tag, "║ Status: ${status.value} ${status.description}")
    Log.d(tag, "╠═══════════════════════════════════════════════════════════")
    Log.d(tag, "║ Headers:")
    headers.forEach { key, values ->
        Log.d(tag, "║   $key: ${values.joinToString()}")
    }
    Log.d(tag, "╠═══════════════════════════════════════════════════════════")

    try {
        val body = bodyAsText()
        Log.d(tag, "║ Response Body:")
        body.lines().take(50).forEach { line -> // Limit to 50 lines
            Log.d(tag, "║   $line")
        }
    } catch (e: Exception) {
        Log.e(tag, "║ Could not read response body: ${e.message}")
    }

    Log.d(tag, "╚═══════════════════════════════════════════════════════════")
}

fun logRequest(
    tag: String = "API_REQUEST",
    url: String,
    method: String,
    headers: Map<String, String> = emptyMap(),
    body: String? = null
) {
    Log.d(tag, "╔═══════════════════════════════════════════════════════════")
    Log.d(tag, "║ REQUEST")
    Log.d(tag, "║ URL: $url")
    Log.d(tag, "║ Method: $method")
    Log.d(tag, "╠═══════════════════════════════════════════════════════════")

    if (headers.isNotEmpty()) {
        Log.d(tag, "║ Headers:")
        headers.forEach { (key, value) ->
            val displayValue = if (key.equals("Authorization", ignoreCase = true)) {
                value.take(20) + "..."
            } else {
                value
            }
            Log.d(tag, "║   $key: $displayValue")
        }
        Log.d(tag, "╠═══════════════════════════════════════════════════════════")
    }

    if (body != null) {
        Log.d(tag, "║ Request Body:")
        body.lines().take(20).forEach { line ->
            Log.d(tag, "║   $line")
        }
    }

    Log.d(tag, "╚═══════════════════════════════════════════════════════════")
}
