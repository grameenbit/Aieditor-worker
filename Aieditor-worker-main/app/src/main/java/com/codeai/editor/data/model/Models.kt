package com.codeai.editor.data.model

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: MutableList<FileNode> = mutableListOf()
)

data class CodeEdit(
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val newContent: String,
    val description: String
)

data class BuildResult(
    val success: Boolean,
    val output: String,
    val errors: List<BuildError> = emptyList()
)

data class BuildError(
    val file: String,
    val line: Int,
    val message: String,
    val severity: String = "error"
)

data class ProjectFile(
    val path: String,
    val content: String,
    val language: String = detectLanguage(path)
)

fun detectLanguage(path: String): String {
    return when {
        path.endsWith(".kt") -> "kotlin"
        path.endsWith(".java") -> "java"
        path.endsWith(".xml") -> "xml"
        path.endsWith(".gradle") || path.endsWith(".gradle.kts") -> "gradle"
        path.endsWith(".json") -> "json"
        path.endsWith(".py") -> "python"
        path.endsWith(".js") -> "javascript"
        path.endsWith(".ts") -> "typescript"
        path.endsWith(".html") -> "html"
        path.endsWith(".css") -> "css"
        path.endsWith(".md") -> "markdown"
        path.endsWith(".yaml") || path.endsWith(".yml") -> "yaml"
        else -> "text"
    }
}
