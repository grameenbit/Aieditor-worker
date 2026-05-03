package com.codeai.editor.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeai.editor.data.api.GeminiApiClient
import com.codeai.editor.data.model.*
import com.codeai.editor.data.repository.FileRepository
import com.codeai.editor.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsManager(application)
    private val fileRepo = FileRepository()
    private val apiClient = GeminiApiClient()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _currentFile = MutableStateFlow<ProjectFile?>(null)
    val currentFile: StateFlow<ProjectFile?> = _currentFile.asStateFlow()

    private val _fileTree = MutableStateFlow<FileNode?>(null)
    val fileTree: StateFlow<FileNode?> = _fileTree.asStateFlow()

    private val _terminalOutput = MutableStateFlow("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _pendingActions = MutableStateFlow<List<AiAction>>(emptyList())
    val pendingActions: StateFlow<List<AiAction>> = _pendingActions.asStateFlow()

    private val _buildStatus = MutableStateFlow("idle")
    val buildStatus: StateFlow<String> = _buildStatus.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini-3-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _useStreaming = MutableStateFlow(true)
    val useStreaming: StateFlow<Boolean> = _useStreaming.asStateFlow()

    private val _projectPath = MutableStateFlow("")
    val projectPath: StateFlow<String> = _projectPath.asStateFlow()

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent.asStateFlow()

    init {
        viewModelScope.launch {
            settings.apiKey.collect { _apiKey.value = it }
        }
        viewModelScope.launch {
            settings.model.collect { _selectedModel.value = it }
        }
        viewModelScope.launch {
            settings.projectPath.collect {
                _projectPath.value = it
                if (it.isNotEmpty()) refreshFileTree()
            }
        }
    }

    fun saveApiKey(key: String) = viewModelScope.launch {
        settings.saveApiKey(key)
        _apiKey.value = key
    }

    fun saveModel(model: String) = viewModelScope.launch {
        settings.saveModel(model)
        _selectedModel.value = model
    }

    fun setProjectPath(path: String) = viewModelScope.launch {
        settings.saveProjectPath(path)
        _projectPath.value = path
        refreshFileTree()
    }

    fun refreshFileTree() {
        if (_projectPath.value.isNotEmpty()) {
            _fileTree.value = fileRepo.getFileTree(_projectPath.value)
        }
    }

    fun openFile(path: String) {
        if (fileRepo.fileExists(path)) {
            val content = fileRepo.readFile(path)
            _currentFile.value = ProjectFile(path, content)
            _editorContent.value = content
        }
    }

    fun saveCurrentFile() {
        val file = _currentFile.value ?: return
        fileRepo.writeFile(file.path, _editorContent.value)
        _currentFile.value = file.copy(content = _editorContent.value)
        appendTerminal("Saved: ${file.path}")
    }

    fun updateEditorContent(content: String) {
        _editorContent.value = content
    }

    fun toggleStreaming() {
        _useStreaming.value = !_useStreaming.value
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank() || _apiKey.value.isBlank()) return

        val projectContext = buildProjectContext()
        val updatedMessages = _chatMessages.value + ChatMessage("user", userMessage)
        _chatMessages.value = updatedMessages
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val contextMessage = if (projectContext.isNotEmpty()) {
                    "Current project context:\n$projectContext\n\nUser request: $userMessage"
                } else userMessage

                val apiMessages = updatedMessages.dropLast(1) + ChatMessage("user", contextMessage)

                if (_useStreaming.value) {
                    _chatMessages.value = _chatMessages.value + ChatMessage("assistant", "")
                    val streamingIdx = _chatMessages.value.size - 1
                    var fullResponse = ""

                    apiClient.streamMessage(
                        _apiKey.value, _selectedModel.value,
                        apiMessages, AiSystemPrompt.SYSTEM_PROMPT
                    ).collect { chunk ->
                        fullResponse += chunk
                        val updated = _chatMessages.value.toMutableList()
                        updated[streamingIdx] = ChatMessage("assistant", fullResponse)
                        _chatMessages.value = updated
                    }

                    val actions = AiResponseParser.parse(fullResponse)
                    val fileActions = actions.filter { it !is AiAction.Message }
                    if (fileActions.isNotEmpty()) {
                        _pendingActions.value = fileActions
                    }
                } else {
                    val response = apiClient.sendMessage(
                        _apiKey.value, _selectedModel.value,
                        apiMessages, AiSystemPrompt.SYSTEM_PROMPT
                    )

                    val actions = AiResponseParser.parse(response)
                    val messageActions = actions.filterIsInstance<AiAction.Message>()
                    val fileActions = actions.filter { it !is AiAction.Message }

                    val aiText = messageActions.joinToString("\n") { (it as AiAction.Message).text }
                    _chatMessages.value = _chatMessages.value + ChatMessage("assistant", aiText.ifEmpty { response })

                    if (fileActions.isNotEmpty()) {
                        _pendingActions.value = fileActions
                    }
                }
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage("assistant", "Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveAction(action: AiAction) {
        when (action) {
            is AiAction.Edit -> {
                val path = resolveFilePath(action.edit.filePath)
                fileRepo.editLines(path, action.edit.startLine, action.edit.endLine, action.edit.newContent)
                appendTerminal("Edited: $path (lines ${action.edit.startLine}-${action.edit.endLine})")
                if (_currentFile.value?.path == path) openFile(path)
            }
            is AiAction.Create -> {
                val path = resolveFilePath(action.path)
                fileRepo.createFile(path, action.content)
                appendTerminal("Created: $path")
                refreshFileTree()
            }
            is AiAction.Delete -> {
                val path = resolveFilePath(action.path)
                fileRepo.deleteFile(path)
                appendTerminal("Deleted: $path")
                refreshFileTree()
            }
            is AiAction.Rename -> {
                val from = resolveFilePath(action.from)
                val to = resolveFilePath(action.to)
                fileRepo.renameFile(from, to)
                appendTerminal("Renamed: $from -> $to")
                refreshFileTree()
            }
            is AiAction.Message -> {}
        }
        _pendingActions.value = _pendingActions.value - action
    }

    fun rejectAction(action: AiAction) {
        _pendingActions.value = _pendingActions.value - action
        appendTerminal("Rejected action")
    }

    fun createNewFile(path: String, content: String = "") {
        val fullPath = resolveFilePath(path)
        fileRepo.createFile(fullPath, content)
        refreshFileTree()
        appendTerminal("Created: $fullPath")
    }

    fun deleteFile(path: String) {
        fileRepo.deleteFile(path)
        if (_currentFile.value?.path == path) {
            _currentFile.value = null
            _editorContent.value = ""
        }
        refreshFileTree()
        appendTerminal("Deleted: $path")
    }

    fun createDirectory(path: String) {
        val fullPath = resolveFilePath(path)
        fileRepo.createDirectory(fullPath)
        refreshFileTree()
        appendTerminal("Created directory: $fullPath")
    }

    fun appendTerminal(text: String) {
        _terminalOutput.value += "\n$ $text"
    }

    fun clearTerminal() {
        _terminalOutput.value = ""
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    private fun resolveFilePath(path: String): String {
        return if (path.startsWith("/")) path
        else "${_projectPath.value}/$path"
    }

    private fun buildProjectContext(): String {
        val tree = _fileTree.value ?: return ""
        val sb = StringBuilder()
        sb.appendLine("Project structure:")
        buildTreeString(tree, sb, "")
        _currentFile.value?.let {
            sb.appendLine("\nCurrently open file: ${it.path}")
            sb.appendLine("Content:")
            it.content.lines().forEachIndexed { idx, line ->
                sb.appendLine("${idx + 1}: $line")
            }
        }
        return sb.toString()
    }

    private fun buildTreeString(node: FileNode, sb: StringBuilder, indent: String) {
        sb.appendLine("$indent${if (node.isDirectory) "[DIR] " else ""}${node.name}")
        if (node.isDirectory) {
            node.children.forEach { buildTreeString(it, sb, "$indent  ") }
        }
    }
}
