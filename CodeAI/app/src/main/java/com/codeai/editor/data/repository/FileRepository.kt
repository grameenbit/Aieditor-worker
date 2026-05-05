package com.codeai.editor.data.repository

import com.codeai.editor.data.model.FileNode
import java.io.File

class FileRepository {

    fun getFileTree(rootPath: String): FileNode = buildFileNode(File(rootPath))

    private fun buildFileNode(file: File): FileNode {
        val node = FileNode(name = file.name, path = file.absolutePath, isDirectory = file.isDirectory)
        if (file.isDirectory) {
            file.listFiles()
                ?.filter { !it.name.startsWith(".") && it.name != "build" }
                ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name })
                ?.forEach { node.children.add(buildFileNode(it)) }
        }
        return node
    }

    fun readFile(path: String): String = File(path).readText()

    fun writeFile(path: String, content: String) {
        File(path).apply { parentFile?.mkdirs(); writeText(content) }
    }

    fun createFile(path: String, content: String = "") = writeFile(path, content)

    fun deleteFile(path: String): Boolean = File(path).deleteRecursively()

    fun renameFile(oldPath: String, newPath: String): Boolean = File(oldPath).renameTo(File(newPath))

    fun moveFile(sourcePath: String, destPath: String): Boolean {
        val dest = File(destPath)
        dest.parentFile?.mkdirs()
        return File(sourcePath).renameTo(dest)
    }

    fun editLines(path: String, startLine: Int, endLine: Int, newContent: String): Boolean {
        val file = File(path)
        if (!file.exists()) return false
        val lines = file.readLines().toMutableList()
        if (startLine < 1 || startLine > lines.size) return false
        val actualEnd = minOf(endLine, lines.size)
        val newLines = newContent.split("\n")
        for (i in (actualEnd - 1) downTo (startLine - 1)) lines.removeAt(i)
        lines.addAll(startLine - 1, newLines)
        file.writeText(lines.joinToString("\n"))
        return true
    }

    fun fileExists(path: String): Boolean = File(path).exists()

    fun createDirectory(path: String): Boolean = File(path).mkdirs()
}
