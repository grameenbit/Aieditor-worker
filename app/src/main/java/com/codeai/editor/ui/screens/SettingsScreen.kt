package com.codeai.editor.ui.screens

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeai.editor.ui.theme.*
import com.codeai.editor.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val apiKey by viewModel.apiKey.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val projectPath by viewModel.projectPath.collectAsState()
    val useStreaming by viewModel.useStreaming.collectAsState()

    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var projectPathInput by remember(projectPath) { mutableStateOf(projectPath) }
    var modelExpanded by remember { mutableStateOf(false) }

    val models = listOf(
        "gemini-3-flash" to "Gemini 3 Flash (Fast)",
        "gemini-3.1-flash-lite" to "Gemini 3.1 Flash Lite (Budget)",
        "gemini-2.5-pro" to "Gemini 2.5 Pro (Reasoning)",
        "gemini-3.1-pro" to "Gemini 3.1 Pro (Most Powerful)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", color = EditorText, fontSize = 24.sp)

        Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Gemini API Key", color = EditorText, fontSize = 14.sp)
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = { Text("Enter your API key", color = EditorTextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = EditorText,
                        unfocusedTextColor = EditorText,
                        focusedBorderColor = EditorPrimary,
                        unfocusedBorderColor = EditorSurfaceVariant,
                        cursorColor = EditorPrimary
                    ),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.saveApiKey(apiKeyInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)
                ) { Text("Save API Key") }
            }
        }

        Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AI Model", color = EditorText, fontSize = 14.sp)
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it }
                ) {
                    OutlinedTextField(
                        value = models.find { it.first == selectedModel }?.second ?: selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = EditorText,
                            unfocusedTextColor = EditorText,
                            focusedBorderColor = EditorPrimary,
                            unfocusedBorderColor = EditorSurfaceVariant
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        models.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    viewModel.saveModel(id)
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Project Path", color = EditorText, fontSize = 14.sp)
                OutlinedTextField(
                    value = projectPathInput,
                    onValueChange = { projectPathInput = it },
                    placeholder = {
                        Text(
                            Environment.getExternalStorageDirectory().absolutePath + "/MyProject",
                            color = EditorTextDim
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = EditorText,
                        unfocusedTextColor = EditorText,
                        focusedBorderColor = EditorPrimary,
                        unfocusedBorderColor = EditorSurfaceVariant,
                        cursorColor = EditorPrimary
                    ),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.setProjectPath(projectPathInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)
                ) { Text("Open Project") }
            }
        }

        Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Streaming Response", color = EditorText, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        if (useStreaming) "Enabled — real-time typing effect"
                        else "Disabled — wait for full response",
                        color = EditorTextDim, fontSize = 12.sp
                    )
                    Switch(
                        checked = useStreaming,
                        onCheckedChange = { viewModel.toggleStreaming() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EditorPrimary,
                            checkedTrackColor = EditorPrimary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}
