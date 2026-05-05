package com.codeai.editor.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "code_ai_settings")

class SettingsManager(private val context: Context) {
    companion object {
        val API_KEY = stringPreferencesKey("gemini_api_key")
        val MODEL = stringPreferencesKey("gemini_model")
        val PROJECT_PATH = stringPreferencesKey("project_path")
        val PROVIDER = stringPreferencesKey("ai_provider")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val CUSTOM_MODELS = stringPreferencesKey("custom_models_json")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val model: Flow<String> = context.dataStore.data.map { it[MODEL] ?: "gemini-2.0-flash" }
    val projectPath: Flow<String> = context.dataStore.data.map { it[PROJECT_PATH] ?: "" }
    val provider: Flow<String> = context.dataStore.data.map { it[PROVIDER] ?: "GEMINI" }
    val openRouterApiKey: Flow<String> = context.dataStore.data.map { it[OPENROUTER_API_KEY] ?: "" }
    val customModels: Flow<String> = context.dataStore.data.map { it[CUSTOM_MODELS] ?: "[]" }

    suspend fun saveApiKey(key: String) { context.dataStore.edit { it[API_KEY] = key } }
    suspend fun saveModel(model: String) { context.dataStore.edit { it[MODEL] = model } }
    suspend fun saveProjectPath(path: String) { context.dataStore.edit { it[PROJECT_PATH] = path } }
    suspend fun saveProvider(provider: String) { context.dataStore.edit { it[PROVIDER] = provider } }
    suspend fun saveOpenRouterApiKey(key: String) { context.dataStore.edit { it[OPENROUTER_API_KEY] = key } }
    suspend fun saveCustomModels(json: String) { context.dataStore.edit { it[CUSTOM_MODELS] = json } }
}
