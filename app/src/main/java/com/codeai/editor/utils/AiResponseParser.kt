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
                    val editStart = remaining.indexOf("===EDIT===")
                    val beforeEdit = remaining.substring(0, editStart).trim()
                    if (beforeEdit.isNotEmpty()) actions.add(AiAction.Message(beforeEdit))

                    val editEnd = remaining.indexOf("===END_EDIT===")
                    if (editEnd == -1) {
                        actions.add(AiAction.Message(remaining))
                        break
                    }
                    val editBlock = remaining.substring(editStart + 10, editEnd).trim()
                    parseEditBlock(editBlock)?.let { actions.add(it) }
                    remaining = remaining.substring(editEnd + 14).trim()
                }
                remaining.contains("===CREATE===") -> {
                    val createStart = remaining.indexOf("===CREATE===")
                    val before = remaining.substring(0, createStart).trim()
                    if (before.isNotEmpty()) actions.add(AiAction.Message(before))

                    val createEnd = remaining.indexOf("===END_CREATE===")
                    if (createEnd == -1) {
                        actions.add(AiAction.Message(remaining))
                        break
                    }
                    val block = remaining.substring(createStart + 12, createEnd).trim()
                    parseCreateBlock(block)?.let { actions.add(it) }
                    remaining = remaining.substring(createEnd + 16).trim()
                }
                remaining.contains("===DELETE===") -> {
                    val delStart = remaining.indexOf("===DELETE===")
                    val before = remaining.substring(0, delStart).trim()
                    if (before.isNotEmpty()) actions.add(AiAction.Message(before))

                    val delEnd = remaining.indexOf("===END_DELETE===")
                    if (delEnd == -1) {
                        actions.add(AiAction.Message(remaining))
                        break
                    }
                    val block = remaining.substring(delStart + 12, delEnd).trim()
                    parseDeleteBlock(block)?.let { actions.add(it) }
                    remaining = remaining.substring(delEnd + 16).trim()
                }
                remaining.contains("===RENAME===") -> {
                    val renStart = remaining.indexOf("===RENAME===")
                    val before = remaining.substring(0, renStart).trim()
                    if (before.isNotEmpty()) actions.add(AiAction.Message(before))

                    val renEnd = remaining.indexOf("===END_RENAME===")
                    if (renEnd == -1) {
                        actions.add(AiAction.Message(remaining))
                        break
                    }
                    val block = remaining.substring(renStart + 12, renEnd).trim()
                    parseRenameBlock(block)?.let { actions.add(it) }
                    remaining = remaining.substring(renEnd + 16).trim()
                }
                else -> {
                    actions.add(AiAction.Message(remaining.trim()))
                    break
                }
            }
        }
        return actions
    }

    private fun parseEditBlock(block: String): AiAction.Edit? {
        val lines = block.lines()
        var file = ""
        var startLine = 0
        var endLine = 0
        var contentStartIdx = -1

        for ((idx, line) in lines.withIndex()) {
            when {
                line.startsWith("FILE:") -> file = line.substringAfter("FILE:").trim()
                line.startsWith("START_LINE:") -> startLine = line.substringAfter("START_LINE:").trim().toIntOrNull() ?: 0
                line.startsWith("END_LINE:") -> endLine = line.substringAfter("END_LINE:").trim().toIntOrNull() ?: 0
                line.startsWith("CONTENT:") -> { contentStartIdx = idx + 1; break }
            }
        }

        if (file.isEmpty() || contentStartIdx == -1) return null
        val content = lines.drop(contentStartIdx).joinToString("\n")
        return AiAction.Edit(CodeEdit(file, startLine, endLine, content, "AI edit"))
    }

    private fun parseCreateBlock(block: String): AiAction.Create? {
        val lines = block.lines()
        var file = ""
        var contentStartIdx = -1

        for ((idx, line) in lines.withIndex()) {
            when {
                line.startsWith("FILE:") -> file = line.substringAfter("FILE:").trim()
                line.startsWith("CONTENT:") -> { contentStartIdx = idx + 1; break }
            }
        }

        if (file.isEmpty() || contentStartIdx == -1) return null
        val content = lines.drop(contentStartIdx).joinToString("\n")
        return AiAction.Create(file, content)
    }

    private fun parseDeleteBlock(block: String): AiAction.Delete? {
        val lines = block.lines()
        for (line in lines) {
            if (line.startsWith("FILE:")) {
                return AiAction.Delete(line.substringAfter("FILE:").trim())
            }
        }
        return null
    }

    private fun parseRenameBlock(block: String): AiAction.Rename? {
        val lines = block.lines()
        var from = ""
        var to = ""
        for (line in lines) {
            when {
                line.startsWith("FROM:") -> from = line.substringAfter("FROM:").trim()
                line.startsWith("TO:") -> to = line.substringAfter("TO:").trim()
            }
        }
        return if (from.isNotEmpty() && to.isNotEmpty()) AiAction.Rename(from, to) else null
    }
}
