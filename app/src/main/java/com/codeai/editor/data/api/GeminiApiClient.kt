package com.codeai.editor.data.api

import com.codeai.editor.data.model.ChatMessage
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val contents = messages.map { msg ->
            mapOf(
                "role" to if (msg.role == "user") "user" else "model",
                "parts" to listOf(mapOf("text" to msg.content))
            )
        }

        val body = buildMap {
            put("contents", contents)
            put("systemInstruction", mapOf(
                "parts" to listOf(mapOf("text" to systemPrompt))
            ))
            put("generationConfig", mapOf(
                "temperature" to 0.7,
                "topP" to 0.95,
                "topK" to 40,
                "maxOutputTokens" to 8192
            ))
        }

        val json = gson.toJson(body)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("API Error ${response.code}: $responseBody")
        }

        val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
        jsonResponse
            .getAsJsonArray("candidates")
            .get(0).asJsonObject
            .getAsJsonObject("content")
            .getAsJsonArray("parts")
            .get(0).asJsonObject
            .get("text").asString
    }
}
