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
import java.util.concurrent.TimeUnit

class OpenRouterApiClient : AiProviderClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
    }

    private fun buildMessages(messages: List<ChatMessage>, systemPrompt: String): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        result.add(mapOf("role" to "system", "content" to systemPrompt))
        messages.forEach { msg -> result.add(mapOf("role" to msg.role, "content" to msg.content)) }
        return result
    }

    override suspend fun sendMessage(
        apiKey: String, model: String,
        messages: List<ChatMessage>, systemPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val body = mapOf("model" to model, "messages" to buildMessages(messages, systemPrompt))
        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://codeai.app")
            .addHeader("X-Title", "CodeAI")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        if (!response.isSuccessful) {
            val msg = try {
                JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("error")
                    ?.get("message")?.asString ?: "HTTP ${response.code}"
            } catch (_: Exception) { "HTTP ${response.code}: $responseBody" }
            throw Exception(msg)
        }
        JsonParser.parseString(responseBody).asJsonObject
            .getAsJsonArray("choices").get(0).asJsonObject
            .getAsJsonObject("message").get("content").asString
    }

    override fun streamMessage(
        apiKey: String, model: String,
        messages: List<ChatMessage>, systemPrompt: String
    ): Flow<String> = flow {
        val body = mapOf(
            "model" to model,
            "messages" to buildMessages(messages, systemPrompt),
            "stream" to true
        )
        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://codeai.app")
            .addHeader("X-Title", "CodeAI")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            val msg = try {
                JsonParser.parseString(errBody).asJsonObject.getAsJsonObject("error")
                    ?.get("message")?.asString ?: "HTTP ${response.code}"
            } catch (_: Exception) { "HTTP ${response.code}: $errBody" }
            throw Exception(msg)
        }
        val reader = response.body?.byteStream()?.bufferedReader()
            ?: throw Exception("Empty stream")
        reader.use {
            var line: String?
            while (it.readLine().also { l -> line = l } != null) {
                val data = line ?: continue
                if (!data.startsWith("data: ")) continue
                val chunk = data.removePrefix("data: ").trim()
                if (chunk.isEmpty() || chunk == "[DONE]") continue
                try {
                    val obj = JsonParser.parseString(chunk).asJsonObject
                    val choices = obj.getAsJsonArray("choices") ?: continue
                    if (choices.size() == 0) continue
                    val content = choices.get(0).asJsonObject
                        .getAsJsonObject("delta")?.get("content")?.asString ?: ""
                    if (content.isNotEmpty()) emit(content)
                } catch (_: Exception) { }
            }
        }
    }.flowOn(Dispatchers.IO)
}
