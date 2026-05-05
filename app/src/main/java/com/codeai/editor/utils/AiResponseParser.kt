package com.codeai.editor.utils

import com.codeai.editor.data.model.CodeEdit

sealed class AiAction {
    data class Edit(val edit: CodeEdit) : AiAction()
    data class Create(val path: String, val content: String) : AiAction()
    data class Delete(val path: String) : AiAction()
    data class Rename(val from: String, val to: String) : AiAction()
    data class Message(val text: String) : AiAction()
}

object AiResponseParser {
    fun parse(response: String): List<AiAction> {
        val actions = mutableListOf<AiAction>()
        var remaining = response

        while (remaining.isNotEmpty()) {
            when {
                remaining.contains("===EDIT===") -> {
                    val start = remaining.indexOf("===EDIT===")
                    val before = remaining.substring(0, start).trim()
                    if (before.isNotEmpty()) actions.add(AiAction.Message(before))
                    val end = remaining.indexOf("===END_EDIT===")
                    if (end == -1) { actions.add(AiAction.Message(remaining)); break }
                    val block = remaining.substring(start + 10, end).trim()
                    parseEditBlock(block)?.let { actions.add(it) }
                    remaining = remaining.substring(end + 14).trim()
                }
                remaining.contains("===CREATE===") -> {
                    val start = remaining.indexOf("===CREATE===")
                    val before = remaining.substring(0, start).trim()
                    if (before.isNotEmpty()) actions.add(AiAction.Message(before))
                    val end = remaining.indexOf("===END_CREATE===")
                    if (end == -1) { actions.add(AiAction.Message(remaining)); break }
                    val block = remaining.substring(start + 12, end).trim()
                    parseCreateBlock(block)?.let { actions.add(it) }
                    remaining = remaining.substring(end + 16).trim()
                }
                remaining.contains("===DELETE===") -> {
                    val start = remaining.indexOf("===DELETE===")
                    val before = remaining.substring(0, start).trim()
                    if (before.isNotEmpty()) actions.add(AiAction.Message(before))
                    val end = remaining.indexOf("===END_DELETE===")
                    if (end == -1) { actions.add(AiAction.Message(remaining)); break }
                    val block = remaining.substring(start + 12, end).trim()
                    parseDeleteBlock(block)?.let { actions.add(it) }
                    remaining = remaining.substring(end + 16).trim()
                }
                remaining.contains("===RENAME===") -> {
                    val start = remaining.indexOf("===RENAME===")
                    val before = remaining.substring(0, start).trim()
                    if (before.isNotEmpty()) actions.add(AiAction.Message(before))
                    val end = remaining.indexOf("===END_RENAME===")
                    if (end == -1) { actions.add(AiAction.Message(remaining)); break }
                    val block = remaining.substring(start + 12, end).trim()
                    parseRenameBlock(block)?.let { actions.add(it) }
                    remaining = remaining.substring(end + 16).trim()
                }
                else -> { actions.add(AiAction.Message(remaining.trim())); break }
            }
        }
        return actions
    }

    private fun parseEditBlock(block: String): AiAction.Edit? {
        val lines = block.lines()
        var file = ""; var startLine = 0; var endLine = 0; var contentStartIdx = -1
        for ((idx, line) in lines.withIndex()) {
            when {
                line.startsWith("FILE:") -> file = line.substringAfter("FILE:").trim()
                line.startsWith("START_LINE:") -> startLine = line.substringAfter("START_LINE:").trim().toIntOrNull() ?: 0
                line.startsWith("END_LINE:") -> endLine = line.substringAfter("END_LINE:").trim().toIntOrNull() ?: 0
                line.startsWith("CONTENT:") -> { contentStartIdx = idx + 1; break }
            }
        }
        if (file.isEmpty() || contentStartIdx == -1) return null
        return AiAction.Edit(CodeEdit(file, startLine, endLine, lines.drop(contentStartIdx).joinToString("\n"), "AI edit"))
    }

    private fun parseCreateBlock(block: String): AiAction.Create? {
        val lines = block.lines()
        var file = ""; var contentStartIdx = -1
        for ((idx, line) in lines.withIndex()) {
            when {
                line.startsWith("FILE:") -> file = line.substringAfter("FILE:").trim()
                line.startsWith("CONTENT:") -> { contentStartIdx = idx + 1; break }
            }
        }
        if (file.isEmpty() || contentStartIdx == -1) return null
        return AiAction.Create(file, lines.drop(contentStartIdx).joinToString("\n"))
    }

    private fun parseDeleteBlock(block: String): AiAction.Delete? {
        for (line in block.lines()) {
            if (line.startsWith("FILE:")) return AiAction.Delete(line.substringAfter("FILE:").trim())
        }
        return null
    }

    private fun parseRenameBlock(block: String): AiAction.Rename? {
        var from = ""; var to = ""
        for (line in block.lines()) {
            when {
                line.startsWith("FROM:") -> from = line.substringAfter("FROM:").trim()
                line.startsWith("TO:") -> to = line.substringAfter("TO:").trim()
            }
        }
        return if (from.isNotEmpty() && to.isNotEmpty()) AiAction.Rename(from, to) else null
    }
}
