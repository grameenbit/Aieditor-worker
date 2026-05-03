package com.codeai.editor.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeai.editor.data.api.AiProviderClient
import com.codeai.editor.data.api.GeminiApiClient
import com.codeai.editor.data.api.OpenRouterApiClient
import com.codeai.editor.data.model.*
import com.codeai.editor.data.repository.FileRepository
import com.codeai.editor.utils.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = SettingsManager(application)
    private val fileRepo = FileRepository()
    private val geminiClient = GeminiApiClient()
    private val openRouterClient = OpenRouterApiClient()
    private val gson = Gson()

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
    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent.asStateFlow()

    private val _fileSummaries = MutableStateFlow<Map<String, FileSummary>>(emptyMap())
    val fileSummaries: StateFlow<Map<String, FileSummary>> = _fileSummaries.asStateFlow()

    val apiKey = settings.apiKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val selectedModel = settings.model.stateIn(viewModelScope, SharingStarted.Eagerly, "gemini-2.0-flash")
    val projectPath = settings.projectPath.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val useStreaming = settings.provider.map { it != "NO_STREAM" }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val provider = settings.provider.stateIn(viewModelScope, SharingStarted.Eagerly, "GEMINI")
    val openRouterApiKey = settings.openRouterApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val customModels: StateFlow<List<CustomModel>> = settings.customModels.map { json ->
        try {
            gson.fromJson<List<CustomModel>>(json, object : TypeToken<List<CustomModel>>() {}.type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedCustomModel = MutableStateFlow<CustomModel?>(null)
    val selectedCustomModel: StateFlow<CustomModel?> = _selectedCustomModel.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    init {
        viewModelScope.launch {
            projectPath.collect { path ->
                if (path.isNotEmpty() && File(path).exists()) {
                    refreshFileTree()
                    generateFileSummaries()
                }
            }
        }
    }

    private fun getClient(): AiProviderClient {
        return when {
            _selectedCustomModel.value != null -> openRouterClient
            provider.value == "OPENROUTER" -> openRouterClient
            else -> geminiClient
        }
    }

    private fun getActiveApiKey(): String {
        val custom = _selectedCustomModel.value
        if (custom != null && custom.apiKey.isNotEmpty()) return custom.apiKey
        return when {
            provider.value == "OPENROUTER" -> openRouterApiKey.value
            else -> apiKey.value
        }
    }

    private fun getActiveModel(): String {
        return _selectedCustomModel.value?.id ?: selectedModel.value
    }
    fun sendMessage(text: String, imageBase64: String? = null, attachedFilePath: String? = null) {
        val key = getActiveApiKey()
        if (key.isEmpty()) {
            _chatMessages.value += ChatMessage("assistant", "Please set your API key in Settings first.")
            return
        }

        var messageContent = text
        if (attachedFilePath != null) {
            try {
                val fileContent = fileRepo.readFile(attachedFilePath)
                val fileName = File(attachedFilePath).name
                messageContent += "\n\n[Attached file: $fileName]\n```\n$fileContent\n```"
            } catch (_: Exception) {}
        }

        val userMsg = ChatMessage("user", messageContent, imageBase64 = imageBase64, attachedFilePath = attachedFilePath)
        _chatMessages.value += userMsg
        _isLoading.value = true
        _streamingText.value = ""

        viewModelScope.launch {
            try {
                val contextPrompt = buildContextPrompt()
                val systemPrompt = AiSystemPrompt.SYSTEM_PROMPT + contextPrompt
                val client = getClient()
                val model = getActiveModel()

                val sb = StringBuilder()
                client.streamMessage(key, model, _chatMessages.value, systemPrompt)
                    .catch { e ->
                        sb.append("Error: ${e.message}")
                    }
                    .collect { chunk ->
                        sb.append(chunk)
                        _streamingText.value = sb.toString()
                    }

                val response = sb.toString()
                _streamingText.value = ""
                val actions = AiResponseParser.parse(response)
                val messages = mutableListOf<AiAction.Message>()
                val fileActions = mutableListOf<AiAction>()

                actions.forEach { action ->
                    when (action) {
                        is AiAction.Message -> messages.add(action)
                        else -> fileActions.add(action)
                    }
                }

                val msgText = messages.joinToString("\n") { it.text }
                if (msgText.isNotBlank()) {
                    _chatMessages.value += ChatMessage("assistant", msgText)
                }
                if (fileActions.isNotEmpty()) {
                    _pendingActions.value = fileActions
                }
            } catch (e: Exception) {
                _chatMessages.value += ChatMessage("assistant", "Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun buildContextPrompt(): String {
        val sb = StringBuilder("\n\nCURRENT PROJECT CONTEXT:\n")
        val path = projectPath.value
        if (path.isNotEmpty()) {
            sb.append("Project path: $path\n")
        }
        val file = _currentFile.value
        if (file != null) {
            sb.append("Currently open file: ${file.path}\n")
            sb.append("File content:\n${file.content}\n")
        }
        val summaries = _fileSummaries.value
        if (summaries.isNotEmpty()) {
            sb.append("\nFILE SUMMARIES:\n")
            summaries.values.forEach { s ->
                sb.append("- ${s.relativePath}: ${s.summary}\n")
            }
        }
        return sb.toString()
    }
    fun approveAction(action: AiAction) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = projectPath.value
            when (action) {
                is AiAction.Edit -> {
                    val fullPath = if (action.edit.filePath.startsWith("/")) action.edit.filePath
                        else "$path/${action.edit.filePath}"
                    fileRepo.editLines(fullPath, action.edit.startLine, action.edit.endLine, action.edit.newContent)
                    updateFileSummary(fullPath)
                }
                is AiAction.Create -> {
                    val fullPath = if (action.path.startsWith("/")) action.path else "$path/${action.path}"
                    fileRepo.createFile(fullPath, action.content)
                    updateFileSummary(fullPath)
                }
                is AiAction.Delete -> {
                    val fullPath = if (action.path.startsWith("/")) action.path else "$path/${action.path}"
                    fileRepo.deleteFile(fullPath)
                    _fileSummaries.value = _fileSummaries.value - fullPath
                }
                is AiAction.Rename -> {
                    val fromPath = if (action.from.startsWith("/")) action.from else "$path/${action.from}"
                    val toPath = if (action.to.startsWith("/")) action.to else "$path/${action.to}"
                    fileRepo.renameFile(fromPath, toPath)
                    _fileSummaries.value = _fileSummaries.value - fromPath
                    updateFileSummary(toPath)
                }
                is AiAction.Message -> {}
            }
            withContext(Dispatchers.Main) {
                _pendingActions.value = _pendingActions.value.drop(1)
                refreshFileTree()
                _chatMessages.value += ChatMessage("assistant", "Action applied successfully.")
            }
        }
    }

    fun rejectAction(action: AiAction) {
        _pendingActions.value = _pendingActions.value.drop(1)
        _chatMessages.value += ChatMessage("assistant", "Action rejected.")
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
        _streamingText.value = ""
    }

    fun openFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = fileRepo.readFile(path)
                val pf = ProjectFile(path, content)
                withContext(Dispatchers.Main) {
                    _currentFile.value = pf
                    _editorContent.value = content
                }
            } catch (_: Exception) {}
        }
    }

    fun saveCurrentFile() {
        val file = _currentFile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            fileRepo.writeFile(file.path, _editorContent.value)
            updateFileSummary(file.path)
        }
    }

    fun updateEditorContent(content: String) {
        _editorContent.value = content
    }
    fun refreshFileTree() {
        val path = projectPath.value
        if (path.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tree = fileRepo.getFileTree(path)
                withContext(Dispatchers.Main) { _fileTree.value = tree }
            } catch (_: Exception) {}
        }
    }

    fun createNewFile(relativePath: String) {
        val path = projectPath.value
        if (path.isEmpty() || relativePath.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            fileRepo.createFile("$path/$relativePath", "")
            withContext(Dispatchers.Main) { refreshFileTree() }
        }
    }

    fun createDirectory(relativePath: String) {
        val path = projectPath.value
        if (path.isEmpty() || relativePath.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            fileRepo.createDirectory("$path/$relativePath")
            withContext(Dispatchers.Main) { refreshFileTree() }
        }
    }

    fun deleteFile(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            fileRepo.deleteFile(filePath)
            _fileSummaries.value = _fileSummaries.value - filePath
            withContext(Dispatchers.Main) {
                if (_currentFile.value?.path == filePath) {
                    _currentFile.value = null
                    _editorContent.value = ""
                }
                refreshFileTree()
            }
        }
    }

    fun renameFile(oldPath: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val parent = File(oldPath).parent ?: return@launch
            val newPath = "$parent/$newName"
            fileRepo.renameFile(oldPath, newPath)
            _fileSummaries.value = _fileSummaries.value - oldPath
            withContext(Dispatchers.Main) {
                if (_currentFile.value?.path == oldPath) {
                    openFile(newPath)
                }
                refreshFileTree()
            }
        }
    }

    fun moveFile(sourcePath: String, destDir: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = File(sourcePath).name
            val destPath = "$destDir/$fileName"
            fileRepo.moveFile(sourcePath, destPath)
            _fileSummaries.value = _fileSummaries.value - sourcePath
            withContext(Dispatchers.Main) { refreshFileTree() }
        }
    }

    fun copyFilePath(path: String): String = path
    // Settings
    fun saveApiKey(key: String) { viewModelScope.launch { settings.saveApiKey(key) } }
    fun saveModel(model: String) { viewModelScope.launch { settings.saveModel(model) } }
    fun setProjectPath(path: String) {
        viewModelScope.launch {
            settings.saveProjectPath(path)
            refreshFileTree()
            generateFileSummaries()
        }
    }
    fun toggleStreaming() {}
    fun saveProvider(p: String) { viewModelScope.launch { settings.saveProvider(p) } }
    fun saveOpenRouterApiKey(key: String) { viewModelScope.launch { settings.saveOpenRouterApiKey(key) } }

    fun selectCustomModel(model: CustomModel?) {
        _selectedCustomModel.value = model
    }

    fun addCustomModel(model: CustomModel) {
        viewModelScope.launch {
            val current = customModels.value.toMutableList()
            current.add(model)
            settings.saveCustomModels(gson.toJson(current))
        }
    }

    fun removeCustomModel(modelId: String) {
        viewModelScope.launch {
            val current = customModels.value.filter { it.id != modelId }
            settings.saveCustomModels(gson.toJson(current))
            if (_selectedCustomModel.value?.id == modelId) {
                _selectedCustomModel.value = null
            }
        }
    }

    // Create project
    fun createProject(template: ProjectTemplate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val projectDir = File(template.folderPath, template.appName)
                projectDir.mkdirs()
                generateAndroidProject(projectDir, template)
                withContext(Dispatchers.Main) {
                    setProjectPath(projectDir.absolutePath)
                    _terminalOutput.value += "Project '${template.appName}' created at ${projectDir.absolutePath}\n"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _terminalOutput.value += "Error creating project: ${e.message}\n"
                }
            }
        }
    }
    private fun generateAndroidProject(dir: File, template: ProjectTemplate) {
        val pkgPath = template.packageName.replace(".", "/")
        val isKotlin = template.language.lowercase() == "kotlin"
        val ext = if (isKotlin) "kt" else "java"
        val srcDir = File(dir, "app/src/main/java/$pkgPath")
        srcDir.mkdirs()
        File(dir, "app/src/main/res/layout").mkdirs()
        File(dir, "app/src/main/res/values").mkdirs()
        File(dir, "app/src/main/res/mipmap-hdpi").mkdirs()

        val mainActivity = if (isKotlin) """
package ${template.packageName}

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
""".trimIndent() else """
package ${template.packageName};

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
""".trimIndent()

        File(srcDir, "MainActivity.$ext").writeText(mainActivity)

        File(dir, "app/src/main/res/layout/activity_main.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello ${template.appName}!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        xmlns:app="http://schemas.android.com/apk/res-auto" />
</androidx.constraintlayout.widget.ConstraintLayout>
""".trimIndent())
        File(dir, "app/src/main/res/values/strings.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">${template.appName}</string>
</resources>
""".trimIndent())

        File(dir, "app/src/main/AndroidManifest.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="${template.packageName}">
    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.Light.DarkActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""".trimIndent())

        val pluginLine = if (isKotlin) """    id("com.android.application")
    id("org.jetbrains.kotlin.android")""" else """    id("com.android.application")"""

        File(dir, "app/build.gradle.kts").writeText("""
plugins {
$pluginLine
}

android {
    namespace = "${template.packageName}"
    compileSdk = 34
    defaultConfig {
        applicationId = "${template.packageName}"
        minSdk = ${template.minSdk}
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
""".trimIndent())

        File(dir, "build.gradle.kts").writeText("""
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
""".trimIndent())

        File(dir, "settings.gradle.kts").writeText("""
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "${template.appName}"
include(":app")
""".trimIndent())

        File(dir, "gradle.properties").writeText("""
android.useAndroidX=true
android.nonTransitiveRClass=true
org.gradle.jvmargs=-Xmx2048m
""".trimIndent())
    }
    // Build
    fun buildProject(buildType: BuildType) {
        val path = projectPath.value
        if (path.isEmpty()) {
            _terminalOutput.value += "No project opened.\n"
            return
        }
        _buildStatus.value = "building"
        _terminalOutput.value += "Starting ${buildType.displayName}...\n"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val gradlew = File(path, "gradlew")
                if (!gradlew.exists()) {
                    withContext(Dispatchers.Main) {
                        _terminalOutput.value += "Error: gradlew not found in project.\n"
                        _buildStatus.value = "error"
                    }
                    return@launch
                }
                gradlew.setExecutable(true)
                val process = ProcessBuilder(gradlew.absolutePath, buildType.gradleTask)
                    .directory(File(path))
                    .redirectErrorStream(true)
                    .start()

                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: continue
                    withContext(Dispatchers.Main) {
                        _terminalOutput.value += "$l\n"
                    }
                }
                val exitCode = process.waitFor()
                withContext(Dispatchers.Main) {
                    if (exitCode == 0) {
                        _terminalOutput.value += "\nBuild successful!\n"
                        _buildStatus.value = "success"
                    } else {
                        _terminalOutput.value += "\nBuild failed (exit code: $exitCode)\n"
                        _buildStatus.value = "error"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _terminalOutput.value += "Build error: ${e.message}\n"
                    _buildStatus.value = "error"
                }
            }
        }
    }

    // Import project from device
    fun importProject(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempDir = File(context.cacheDir, "imported_project")
                if (tempDir.exists()) tempDir.deleteRecursively()
                tempDir.mkdirs()

                val zipFile = File(tempDir, "project.zip")
                inputStream?.use { input ->
                    zipFile.outputStream().use { output -> input.copyTo(output) }
                }

                val destDir = File(android.os.Environment.getExternalStorageDirectory(), "CodeAI_Projects/imported")
                destDir.mkdirs()

                extractZip(zipFile, destDir)
                zipFile.delete()

                withContext(Dispatchers.Main) {
                    setProjectPath(destDir.absolutePath)
                    _terminalOutput.value += "Project imported to ${destDir.absolutePath}\n"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _terminalOutput.value += "Import error: ${e.message}\n"
                }
            }
        }
    }
    private fun extractZip(zipFile: File, destDir: File) {
        java.util.zip.ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun importProjectFolder(path: String) {
        if (File(path).exists() && File(path).isDirectory) {
            setProjectPath(path)
            _terminalOutput.value += "Project opened from $path\n"
        } else {
            _terminalOutput.value += "Invalid project path: $path\n"
        }
    }

    // File summaries for AI context
    private fun generateFileSummaries() {
        val path = projectPath.value
        if (path.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val root = File(path)
            val summaries = mutableMapOf<String, FileSummary>()
            root.walkTopDown()
                .filter { it.isFile && !it.path.contains("/.") && !it.path.contains("/build/") }
                .filter { it.length() < 100_000 }
                .take(200)
                .forEach { file ->
                    val relative = file.relativeTo(root).path
                    val lang = detectLanguage(file.path)
                    val content = try { file.readText() } catch (_: Exception) { return@forEach }
                    val summary = generateQuickSummary(file.name, lang, content)
                    summaries[file.absolutePath] = FileSummary(
                        path = file.absolutePath,
                        relativePath = relative,
                        summary = summary,
                        lastModified = file.lastModified()
                    )
                }
            withContext(Dispatchers.Main) {
                _fileSummaries.value = summaries
            }
        }
    }

    private fun generateQuickSummary(name: String, lang: String, content: String): String {
        val lines = content.lines()
        val lineCount = lines.size
        val imports = lines.count { it.trimStart().startsWith("import ") }
        val classes = lines.count { it.contains("class ") || it.contains("interface ") || it.contains("object ") }
        val functions = lines.count { it.contains("fun ") || it.contains("void ") || it.contains("function ") }
        return "$lang file, ${lineCount}L, ${imports} imports, ${classes} types, ${functions} functions"
    }

    private fun updateFileSummary(filePath: String) {
        val path = projectPath.value
        if (path.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists()) return@launch
            val relative = try { file.relativeTo(File(path)).path } catch (_: Exception) { file.name }
            val content = try { file.readText() } catch (_: Exception) { return@launch }
            val lang = detectLanguage(filePath)
            val summary = generateQuickSummary(file.name, lang, content)
            val fs = FileSummary(filePath, relative, summary, file.lastModified())
            withContext(Dispatchers.Main) {
                _fileSummaries.value = _fileSummaries.value + (filePath to fs)
            }
        }
    }

    fun readImageAsBase64(uri: Uri): String? {
        return try {
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
        } catch (_: Exception) { null }
    }
}









