package com.codeai.editor.data.api

import com.codeai.editor.data.model.ChatMessage
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

class GeminiApiClient : AiProviderClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    companion object {
        private const val BASE_URL_V1 = "https://generativelanguage.googleapis.com/v1/models"
        private const val BASE_URL_V1BETA = "https://generativelanguage.googleapis.com/v1beta/models"
        private val MODELS_WITHOUT_SYSTEM_INSTRUCTION = setOf("gemini-1.0-pro", "gemini-pro")
    }

    private fun supportsSystemInstruction(model: String) =
        MODELS_WITHOUT_SYSTEM_INSTRUCTION.none { model.contains(it) }

    private fun getBaseUrl(model: String) =
        if (supportsSystemInstruction(model)) BASE_URL_V1BETA else BASE_URL_V1

    private fun buildRequestBody(
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String,
        maxOutputTokens: Int = 8192,
        temperature: Double = 0.7
    ): String {
        val contents = messages.map { msg ->
            mapOf(
                "role" to if (msg.role == "user") "user" else "model",
                "parts" to buildParts(msg)
            )
        }

        val useSystemInstruction = supportsSystemInstruction(model)
        val finalContents = if (!useSystemInstruction) {
            listOf(
                mapOf("role" to "user", "parts" to listOf(mapOf("text" to "[System]\n$systemPrompt"))),
                mapOf("role" to "model", "parts" to listOf(mapOf("text" to "Understood.")))
            ) + contents
        } else contents

        val body = buildMap<String, Any> {
            put("contents", finalContents)
            if (useSystemInstruction) {
                put("systemInstruction", mapOf("parts" to listOf(mapOf("text" to systemPrompt))))
            }
            put("generationConfig", mapOf(
                "temperature" to temperature,
                "topP" to 0.95,
                "topK" to 40,
                "maxOutputTokens" to maxOutputTokens
            ))
            put("safetySettings", listOf(
                mapOf("category" to "HARM_CATEGORY_HARASSMENT", "threshold" to "BLOCK_NONE"),
                mapOf("category" to "HARM_CATEGORY_HATE_SPEECH", "threshold" to "BLOCK_NONE"),
                mapOf("category" to "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold" to "BLOCK_NONE"),
                mapOf("category" to "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold" to "BLOCK_NONE")
            ))
        }
        return gson.toJson(body)
    }

    private fun buildParts(msg: ChatMessage): List<Map<String, Any>> {
        val parts = mutableListOf<Map<String, Any>>()
        if (msg.imageBase64 != null) {
            parts.add(mapOf(
                "inlineData" to mapOf(
                    "mimeType" to "image/jpeg",
                    "data" to msg.imageBase64
                )
            ))
        }
        parts.add(mapOf("text" to msg.content))
        return parts
    }

    private fun parseResponseText(responseBody: String): String {
        val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
        val candidates = jsonResponse.getAsJsonArray("candidates")
        if (candidates == null || candidates.size() == 0) {
            val error = jsonResponse.getAsJsonObject("error")
            if (error != null) throw Exception("API Error: ${error.get("message")?.asString}")
            throw Exception("No response generated")
        }
        return candidates.get(0).asJsonObject
            .getAsJsonObject("content")
            .getAsJsonArray("parts")
            .get(0).asJsonObject
            .get("text").asString
    }

    override suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val url = "${getBaseUrl(model)}/$model:generateContent?key=$apiKey"
        val json = buildRequestBody(model, messages, systemPrompt)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        if (!response.isSuccessful) {
            val msg = try {
                JsonParser.parseString(body).asJsonObject.getAsJsonObject("error")
                    ?.get("message")?.asString ?: "HTTP ${response.code}"
            } catch (_: Exception) { "HTTP ${response.code}: $body" }
            throw Exception(msg)
        }
        parseResponseText(body)
    }

    override fun streamMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String
    ): Flow<String> = flow {
        val url = "${getBaseUrl(model)}/$model:streamGenerateContent?alt=sse&key=$apiKey"
        val json = buildRequestBody(model, messages, systemPrompt)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val body = response.body?.string() ?: ""
            val msg = try {
                JsonParser.parseString(body).asJsonObject.getAsJsonObject("error")
                    ?.get("message")?.asString ?: "HTTP ${response.code}"
            } catch (_: Exception) { "HTTP ${response.code}: $body" }
            throw Exception(msg)
        }
        val reader: BufferedReader = response.body?.byteStream()?.bufferedReader()
            ?: throw Exception("Empty stream")
        reader.use {
            var line: String?
            while (it.readLine().also { l -> line = l } != null) {
                val data = line ?: continue
                if (!data.startsWith("data: ")) continue
                val chunk = data.removePrefix("data: ").trim()
                if (chunk.isEmpty()) continue
                try {
                    val obj = JsonParser.parseString(chunk).asJsonObject
                    val candidates = obj.getAsJsonArray("candidates") ?: continue
                    if (candidates.size() == 0) continue
                    val parts = candidates.get(0).asJsonObject
                        .getAsJsonObject("content")?.getAsJsonArray("parts") ?: continue
                    if (parts.size() > 0) {
                        val text = parts.get(0).asJsonObject.get("text")?.asString ?: ""
                        if (text.isNotEmpty()) emit(text)
                    }
                } catch (_: Exception) { }
            }
        }
    }.flowOn(Dispatchers.IO)
}
