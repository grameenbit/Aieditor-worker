package com.codeai.editor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeai.editor.ui.components.*
import com.codeai.editor.ui.theme.*
import com.codeai.editor.ui.viewmodel.MainViewModel
import com.codeai.editor.utils.AiAction

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentFile by viewModel.currentFile.collectAsState()
    val fileTree by viewModel.fileTree.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingActions by viewModel.pendingActions.collectAsState()
    val editorContent by viewModel.editorContent.collectAsState()
    val projectPath by viewModel.projectPath.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showFileExplorer by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewDirDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    if (pendingActions.isNotEmpty()) {
        ActionApprovalDialog(
            action = pendingActions.first(),
            onApprove = { viewModel.approveAction(pendingActions.first()) },
            onReject = { viewModel.rejectAction(pendingActions.first()) }
        )
    }

    showDeleteConfirm?.let { path ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = EditorSurface,
            title = { Text("Delete?", color = EditorText) },
            text = { Text("Delete $path?", color = EditorTextDim) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteFile(path); showDeleteConfirm = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = null }) { Text("Cancel", color = EditorText) }
            }
        )
    }

    if (showNewFileDialog) {
        var fileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            containerColor = EditorSurface,
            title = { Text("New File", color = EditorText) },
            text = {
                OutlinedTextField(
                    value = fileName, onValueChange = { fileName = it },
                    placeholder = { Text("path/to/file.kt", color = EditorTextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = EditorText, unfocusedTextColor = EditorText,
                        focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant,
                        cursorColor = EditorPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createNewFile(fileName); showNewFileDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)
                ) { Text("Create") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewFileDialog = false }) { Text("Cancel", color = EditorText) }
            }
        )
    }

    if (showNewDirDialog) {
        var dirName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDirDialog = false },
            containerColor = EditorSurface,
            title = { Text("New Folder", color = EditorText) },
            text = {
                OutlinedTextField(
                    value = dirName, onValueChange = { dirName = it },
                    placeholder = { Text("folder/name", color = EditorTextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = EditorText, unfocusedTextColor = EditorText,
                        focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant,
                        cursorColor = EditorPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createDirectory(dirName); showNewDirDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)
                ) { Text("Create") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewDirDialog = false }) { Text("Cancel", color = EditorText) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(EditorBg)) {
        // Top bar
        Surface(color = EditorSurface, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showFileExplorer = !showFileExplorer }) {
                    Icon(Icons.Default.Menu, "Files", tint = EditorText)
                }
                Text(
                    currentFile?.path?.substringAfterLast("/") ?: "Code AI",
                    color = EditorText, fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                if (currentFile != null) {
                    IconButton(onClick = { viewModel.saveCurrentFile() }) {
                        Icon(Icons.Default.Save, "Save", tint = EditorPrimary)
                    }
                }
                IconButton(onClick = { showNewFileDialog = true }) {
                    Icon(Icons.Default.NoteAdd, "New File", tint = EditorText)
                }
                IconButton(onClick = { showNewDirDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, "New Folder", tint = EditorText)
                }
            }
        }

        // Main content
        Row(modifier = Modifier.weight(1f)) {
            if (showFileExplorer) {
                FileExplorer(
                    rootNode = fileTree,
                    onFileClick = { viewModel.openFile(it) },
                    onDeleteFile = { showDeleteConfirm = it },
                    modifier = Modifier.width(250.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Tab bar
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = EditorSurface,
                    contentColor = EditorPrimary
                ) {
                    Tab(selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, null, Modifier.size(16.dp), tint = EditorText)
                            Spacer(Modifier.width(4.dp))
                            Text("Editor", color = EditorText, fontSize = 12.sp)
                        }
                    }
                    Tab(selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, null, Modifier.size(16.dp), tint = EditorText)
                            Spacer(Modifier.width(4.dp))
                            Text("AI Chat", color = EditorText, fontSize = 12.sp)
                        }
                    }
                    Tab(selectedTab == 2, onClick = { selectedTab = 2 }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, null, Modifier.size(16.dp), tint = EditorText)
                            Spacer(Modifier.width(4.dp))
                            Text("Terminal", color = EditorText, fontSize = 12.sp)
                        }
                    }
                    Tab(selectedTab == 3, onClick = { selectedTab = 3 }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, null, Modifier.size(16.dp), tint = EditorText)
                            Spacer(Modifier.width(4.dp))
                            Text("Settings", color = EditorText, fontSize = 12.sp)
                        }
                    }
                }

                // Tab content
                when (selectedTab) {
                    0 -> {
                        if (currentFile != null) {
                            CodeEditor(
                                content = editorContent,
                                language = currentFile!!.language,
                                onContentChange = { viewModel.updateEditorContent(it) }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(EditorBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Code, null, Modifier.size(64.dp), tint = EditorPrimary.copy(alpha = 0.5f))
                                    Spacer(Modifier.height(16.dp))
                                    Text("Code AI", color = EditorText, fontSize = 24.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        if (projectPath.isEmpty()) "Open a project in Settings to get started"
                                        else "Select a file from the explorer",
                                        color = EditorTextDim, fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                    1 -> AiChatPanel(
                        messages = chatMessages,
                        isLoading = isLoading,
                        onSendMessage = { viewModel.sendMessage(it) },
                        onClearChat = { viewModel.clearChat() }
                    )
                    2 -> TerminalPanel(output = terminalOutput)
                    3 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
