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
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val model: Flow<String> = context.dataStore.data.map { it[MODEL] ?: "gemini-2.0-flash" }
    val projectPath: Flow<String> = context.dataStore.data.map { it[PROJECT_PATH] ?: "" }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    suspend fun saveModel(model: String) {
        context.dataStore.edit { it[MODEL] = model }
    }

    suspend fun saveProjectPath(path: String) {
        context.dataStore.edit { it[PROJECT_PATH] = path }
    }
}
