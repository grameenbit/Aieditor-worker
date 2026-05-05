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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeai.editor.data.model.*
import com.codeai.editor.ui.theme.*
import com.codeai.editor.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val apiKey by viewModel.apiKey.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val projectPath by viewModel.projectPath.collectAsState()
    val providerStr by viewModel.provider.collectAsState()
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsState()
    val customModels by viewModel.customModels.collectAsState()

    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var projectPathInput by remember(projectPath) { mutableStateOf(projectPath) }
    var modelExpanded by remember { mutableStateOf(false) }
    var selectedProvider by remember(providerStr) {
        mutableStateOf(if (providerStr == "OPENROUTER") AiProvider.OPENROUTER else AiProvider.GEMINI)
    }
    var orApiKeyInput by remember(openRouterApiKey) { mutableStateOf(openRouterApiKey) }
    var showAddModel by remember { mutableStateOf(false) }
    var showCreateProject by remember { mutableStateOf(false) }

    val geminiModels = listOf(
        "gemini-2.5-flash" to "Gemini 2.5 Flash",
        "gemini-2.5-pro" to "Gemini 2.5 Pro",
        "gemini-2.0-flash" to "Gemini 2.0 Flash",
        "gemini-1.5-pro" to "Gemini 1.5 Pro"
    )

    if (showCreateProject) {
        CreateProjectDialog(onDismiss = { showCreateProject = false }, onCreate = { template ->
            viewModel.createProject(template); showCreateProject = false
        })
    }
    if (showAddModel) {
        AddModelDialog(onDismiss = { showAddModel = false }, onAdd = { model ->
            viewModel.addCustomModel(model); showAddModel = false
        })
    }

    Column(
        modifier = Modifier.fillMaxSize().background(EditorBg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", color = EditorText, fontSize = 24.sp)

        // Provider
        Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AI Provider", color = EditorText, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedProvider == AiProvider.GEMINI, onClick = { selectedProvider = AiProvider.GEMINI; viewModel.saveProvider("GEMINI") }, label = { Text("Gemini") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EditorPrimary, selectedLabelColor = EditorText))
                    FilterChip(selected = selectedProvider == AiProvider.OPENROUTER, onClick = { selectedProvider = AiProvider.OPENROUTER; viewModel.saveProvider("OPENROUTER") }, label = { Text("OpenRouter") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EditorPrimary, selectedLabelColor = EditorText))
                }
            }
        }

        // Gemini settings
        if (selectedProvider == AiProvider.GEMINI) {
            Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Gemini API Key", color = EditorText, fontSize = 14.sp)
                    OutlinedTextField(value = apiKeyInput, onValueChange = { apiKeyInput = it }, placeholder = { Text("Enter your Gemini API key", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary), singleLine = true)
                    Button(onClick = { viewModel.saveApiKey(apiKeyInput) }, colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)) { Text("Save API Key") }
                }
            }
            Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Gemini Model", color = EditorText, fontSize = 14.sp)
                    ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it }) {
                        OutlinedTextField(
                            value = geminiModels.find { it.first == selectedModel }?.second ?: selectedModel,
                            onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant)
                        )
                        ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                            geminiModels.forEach { (id, name) ->
                                DropdownMenuItem(text = { Text(name) }, onClick = { viewModel.saveModel(id); modelExpanded = false })
                            }
                        }
                    }
                }
            }
        }

        // OpenRouter settings
        if (selectedProvider == AiProvider.OPENROUTER) {
            Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("OpenRouter API Key", color = EditorText, fontSize = 14.sp)
                    OutlinedTextField(value = orApiKeyInput, onValueChange = { orApiKeyInput = it }, placeholder = { Text("sk-or-...", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary), singleLine = true)
                    Button(onClick = { viewModel.saveOpenRouterApiKey(orApiKeyInput) }, colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)) { Text("Save API Key") }
                }
            }
        }

        // Custom Models
        Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Custom Models (OpenRouter)", color = EditorText, fontSize = 14.sp)
                    IconButton(onClick = { showAddModel = true }) { Icon(Icons.Default.Add, "Add Model", tint = EditorPrimary) }
                }
                if (customModels.isEmpty()) Text("No custom models. Tap + to add.", color = EditorTextDim, fontSize = 12.sp)
                customModels.forEach { model ->
                    Surface(color = EditorSurfaceVariant, shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(model.displayName.ifEmpty { model.id }, color = EditorText, fontSize = 13.sp)
                                Text(model.id, color = EditorTextDim, fontSize = 11.sp)
                            }
                            IconButton(onClick = { viewModel.removeCustomModel(model.id) }) { Icon(Icons.Default.Delete, "Remove", tint = ErrorRed, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }

        // Project Path
        Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Project Path", color = EditorText, fontSize = 14.sp)
                OutlinedTextField(value = projectPathInput, onValueChange = { projectPathInput = it }, placeholder = { Text(Environment.getExternalStorageDirectory().absolutePath + "/MyProject", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.setProjectPath(projectPathInput) }, colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)) { Text("Open Project") }
                    OutlinedButton(onClick = { showCreateProject = true }) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp), tint = EditorPrimary)
                        Spacer(Modifier.width(4.dp))
                        Text("Create Project", color = EditorPrimary)
                    }
                }
            }
        }

        // Build
        Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Build", color = EditorText, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BuildType.entries.forEach { bt ->
                        OutlinedButton(onClick = { viewModel.buildProject(bt) }) { Text(bt.displayName, color = EditorText, fontSize = 11.sp) }
                    }
                }
            }
        }

        // Import Project
        Surface(color = EditorSurface, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Import Project", color = EditorText, fontSize = 14.sp)
                Text("Enter path to existing Android project folder.", color = EditorTextDim, fontSize = 12.sp)
                var importPath by remember { mutableStateOf("") }
                OutlinedTextField(value = importPath, onValueChange = { importPath = it }, placeholder = { Text("/storage/emulated/0/MyApp", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary), singleLine = true)
                Button(onClick = { viewModel.importProjectFolder(importPath) }, colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)) {
                    Icon(Icons.Default.FolderOpen, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Import")
                }
            }
        }
    }
}

@Composable
fun CreateProjectDialog(onDismiss: () -> Unit, onCreate: (ProjectTemplate) -> Unit) {
    var appName by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("Kotlin") }
    var packageName by remember { mutableStateOf("com.example.myapp") }
    var minSdk by remember { mutableStateOf("26") }
    var folderPath by remember { mutableStateOf(Environment.getExternalStorageDirectory().absolutePath + "/CodeAI_Projects") }
    var langExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = EditorSurface,
        title = { Text("Create New Project", color = EditorText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = appName, onValueChange = { appName = it }, label = { Text("App Name", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary), singleLine = true)
                ExposedDropdownMenuBox(expanded = langExpanded, onExpandedChange = { langExpanded = it }) {
                    OutlinedTextField(value = language, onValueChange = {}, readOnly = true, label = { Text("Language", color = EditorTextDim) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(langExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant))
                    ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                        listOf("Kotlin", "Java").forEach { lang -> DropdownMenuItem(text = { Text(lang) }, onClick = { language = lang; langExpanded = false }) }
                    }
                }
                OutlinedTextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Package Name", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary), singleLine = true)
                OutlinedTextField(value = minSdk, onValueChange = { minSdk = it }, label = { Text("Min SDK", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary), singleLine = true)
                OutlinedTextField(value = folderPath, onValueChange = { folderPath = it }, label = { Text("Save To", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { if (appName.isNotBlank()) onCreate(ProjectTemplate(appName, language, packageName, minSdk.toIntOrNull() ?: 26, folderPath)) }, colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)) { Text("Create") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel", color = EditorText) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddModelDialog(onDismiss: () -> Unit, onAdd: (CustomModel) -> Unit) {
    var modelId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var modelApiKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = EditorSurface,
        title = { Text("Add Custom Model", color = EditorText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = modelId, onValueChange = { modelId = it }, label = { Text("Model ID", color = EditorTextDim) }, placeholder = { Text("e.g. google/gemini-2.5-flash", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary), singleLine = true)
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name (optional)", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary), singleLine = true)
                OutlinedTextField(value = modelApiKey, onValueChange = { modelApiKey = it }, label = { Text("API Key (optional)", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { if (modelId.isNotBlank()) onAdd(CustomModel(modelId, displayName, AiProvider.OPENROUTER, modelApiKey)) }, colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)) { Text("Add") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel", color = EditorText) } }
    )
}
