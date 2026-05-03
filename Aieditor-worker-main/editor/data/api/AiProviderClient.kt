package com.codeai.editor.data.api

import com.codeai.editor.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AiProviderClient {
    suspend fun sendMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String
    ): String

    fun streamMessage(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        systemPrompt: String
    ): Flow<String>
}
