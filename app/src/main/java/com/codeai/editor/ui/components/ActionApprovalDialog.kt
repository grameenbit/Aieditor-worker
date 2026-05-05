package com.codeai.editor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeai.editor.ui.theme.*
import com.codeai.editor.utils.AiAction

@Composable
fun ActionApprovalDialog(action: AiAction, onApprove: () -> Unit, onReject: () -> Unit) {
    AlertDialog(
        onDismissRequest = onReject,
        containerColor = EditorSurface,
        title = { Text("AI Action Request", color = EditorText) },
        text = {
            Column {
                Text(
                    text = when (action) {
                        is AiAction.Edit -> "Edit: ${action.edit.filePath}\nLines ${action.edit.startLine}-${action.edit.endLine}"
                        is AiAction.Create -> "Create: ${action.path}"
                        is AiAction.Delete -> "Delete: ${action.path}"
                        is AiAction.Rename -> "Rename: ${action.from} → ${action.to}"
                        is AiAction.Message -> action.text
                    },
                    color = EditorText, fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                when (action) {
                    is AiAction.Edit -> {
                        Surface(color = TerminalBg, shape = RoundedCornerShape(8.dp)) {
                            Text(action.edit.newContent.take(600), color = TerminalText, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                    is AiAction.Create -> {
                        Surface(color = TerminalBg, shape = RoundedCornerShape(8.dp)) {
                            Text(action.content.take(600), color = TerminalText, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(onClick = onApprove, colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)) {
                Text("Approve")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onReject) { Text("Reject", color = ErrorRed) }
        }
    )
}
