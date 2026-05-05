package com.codeai.editor.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

object SyntaxHighlighter {
    private val keywordColor = Color(0xFF569CD6)
    private val stringColor = Color(0xFFCE9178)
    private val commentColor = Color(0xFF6A9955)
    private val numberColor = Color(0xFFB5CEA8)
    private val annotationColor = Color(0xFFDCDCAA)
    private val typeColor = Color(0xFF4EC9B0)

    private val kotlinKeywords = setOf(
        "fun", "val", "var", "class", "object", "interface", "package", "import",
        "return", "if", "else", "when", "for", "while", "do", "break", "continue",
        "null", "true", "false", "is", "in", "as", "this", "super", "throw", "try",
        "catch", "finally", "override", "abstract", "open", "sealed", "data",
        "companion", "private", "protected", "public", "internal", "suspend",
        "lateinit", "by", "lazy", "const", "enum", "annotation", "typealias"
    )

    fun highlight(code: String, language: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        when (language) {
            "kotlin", "java" -> highlightKotlinJava(code, builder)
            "xml", "html" -> highlightXml(code, builder)
            else -> builder.append(code)
        }
        return builder.toAnnotatedString()
    }

    private fun highlightKotlinJava(code: String, builder: AnnotatedString.Builder) {
        var i = 0
        while (i < code.length) {
            when {
                code.startsWith("//", i) -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                    builder.pushStyle(SpanStyle(color = commentColor)); builder.append(code.substring(i, end)); builder.pop()
                    i = end
                }
                code.startsWith("/*", i) -> {
                    val end = code.indexOf("*/", i).let { if (it == -1) code.length else it + 2 }
                    builder.pushStyle(SpanStyle(color = commentColor)); builder.append(code.substring(i, end)); builder.pop()
                    i = end
                }
                code[i] == '"' -> {
                    val end = findStringEnd(code, i)
                    builder.pushStyle(SpanStyle(color = stringColor)); builder.append(code.substring(i, end)); builder.pop()
                    i = end
                }
                code[i] == '@' -> {
                    val end = findWordEnd(code, i + 1)
                    builder.pushStyle(SpanStyle(color = annotationColor)); builder.append(code.substring(i, end)); builder.pop()
                    i = end
                }
                code[i].isDigit() -> {
                    val end = findNumberEnd(code, i)
                    builder.pushStyle(SpanStyle(color = numberColor)); builder.append(code.substring(i, end)); builder.pop()
                    i = end
                }
                code[i].isLetter() || code[i] == '_' -> {
                    val end = findWordEnd(code, i)
                    val word = code.substring(i, end)
                    when {
                        word in kotlinKeywords -> {
                            builder.pushStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold))
                            builder.append(word); builder.pop()
                        }
                        word[0].isUpperCase() -> {
                            builder.pushStyle(SpanStyle(color = typeColor)); builder.append(word); builder.pop()
                        }
                        else -> builder.append(word)
                    }
                    i = end
                }
                else -> { builder.append(code[i].toString()); i++ }
            }
        }
    }

    private fun highlightXml(code: String, builder: AnnotatedString.Builder) {
        var i = 0
        while (i < code.length) {
            when {
                code.startsWith("<!--", i) -> {
                    val end = code.indexOf("-->", i).let { if (it == -1) code.length else it + 3 }
                    builder.pushStyle(SpanStyle(color = commentColor)); builder.append(code.substring(i, end)); builder.pop()
                    i = end
                }
                code[i] == '<' -> {
                    builder.pushStyle(SpanStyle(color = keywordColor)); builder.append("<"); builder.pop()
                    i++
                    val end = findWordEnd(code, i)
                    if (end > i) {
                        builder.pushStyle(SpanStyle(color = typeColor)); builder.append(code.substring(i, end)); builder.pop()
                        i = end
                    }
                }
                code[i] == '>' -> {
                    builder.pushStyle(SpanStyle(color = keywordColor)); builder.append(">"); builder.pop(); i++
                }
                code[i] == '"' -> {
                    val end = findStringEnd(code, i)
                    builder.pushStyle(SpanStyle(color = stringColor)); builder.append(code.substring(i, end)); builder.pop()
                    i = end
                }
                else -> { builder.append(code[i].toString()); i++ }
            }
        }
    }

    private fun findStringEnd(code: String, start: Int): Int {
        var i = start + 1
        while (i < code.length) { if (code[i] == '"' && code[i - 1] != '\\') return i + 1; i++ }
        return code.length
    }

    private fun findWordEnd(code: String, start: Int): Int {
        var i = start
        while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) i++
        return i
    }

    private fun findNumberEnd(code: String, start: Int): Int {
        var i = start
        while (i < code.length && (code[i].isDigit() || code[i] == '.' || code[i] == 'f' || code[i] == 'L')) i++
        return i
    }
}
