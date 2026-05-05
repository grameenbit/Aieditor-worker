package com.codeai.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeai.editor.ui.theme.*

@Composable
fun CodeEditor(
    content: String,
    language: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val lineCount = content.lines().size

    Row(modifier = modifier.fillMaxSize().background(EditorBg)) {
        // Line numbers
        Column(
            modifier = Modifier
                .width(48.dp)
                .verticalScroll(verticalScroll)
                .background(EditorSurface)
                .padding(end = 8.dp, top = 8.dp)
        ) {
            for (i in 1..lineCount) {
                Text(
                    text = i.toString(),
                    color = EditorTextDim,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        // Code area
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
                .padding(8.dp)
        ) {
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                readOnly = readOnly,
                textStyle = TextStyle(
                    color = EditorText,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(EditorPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
