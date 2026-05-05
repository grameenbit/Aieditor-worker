package com.codeai.editor.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeai.editor.data.model.ChatMessage
import com.codeai.editor.data.model.CustomModel
import com.codeai.editor.ui.theme.*

@Composable
fun AiChatPanel(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    streamingText: String,
    onSendMessage: (String, String?, String?) -> Unit,
    onClearChat: () -> Unit,
    customModels: List<CustomModel>,
    selectedCustomModel: CustomModel?,
    onSelectModel: (CustomModel?) -> Unit,
    onReadImage: (Uri) -> String?,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var imageFileName by remember { mutableStateOf<String?>(null) }
    var attachedFilePath by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val base64 = onReadImage(it)
            if (base64 != null) {
                imageBase64 = base64
                imageFileName = it.lastPathSegment ?: "image"
            }
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { attachedFilePath = it.path }
    }

    LaunchedEffect(messages.size, streamingText) {
        val targetIdx = messages.size - 1 + if (streamingText.isNotEmpty()) 1 else 0
        if (targetIdx >= 0) listState.animateScrollToItem(targetIdx)
    }

    Column(modifier = modifier.fillMaxSize().background(EditorSurface)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI ASSISTANT", color = EditorTextDim, fontSize = 11.sp)
            TextButton(onClick = onClearChat) { Text("Clear", color = EditorTextDim, fontSize = 11.sp) }
        }

        // Custom model chips
        if (customModels.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCustomModel == null,
                    onClick = { onSelectModel(null) },
                    label = { Text("Default", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EditorPrimary, selectedLabelColor = EditorText, labelColor = EditorTextDim)
                )
                customModels.forEach { model ->
                    FilterChip(
                        selected = selectedCustomModel?.id == model.id,
                        onClick = { onSelectModel(model) },
                        label = { Text(model.displayName.ifEmpty { model.id.substringAfterLast("/") }, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EditorPrimary, selectedLabelColor = EditorText, labelColor = EditorTextDim)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg -> ChatBubble(msg) }
            if (streamingText.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                        Text("AI", color = EditorTextDim, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        Surface(shape = RoundedCornerShape(12.dp), color = EditorSurfaceVariant, modifier = Modifier.widthIn(max = 300.dp)) {
                            Text(streamingText, color = EditorText, fontSize = 13.sp, modifier = Modifier.padding(10.dp), lineHeight = 18.sp)
                        }
                    }
                }
            }
            if (isLoading && streamingText.isEmpty()) {
                item { Text("Thinking...", color = EditorTextDim, fontSize = 13.sp, modifier = Modifier.padding(8.dp)) }
            }
        }

        // Attachment preview
        if (imageFileName != null || attachedFilePath != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                imageFileName?.let { name ->
                    Surface(color = EditorPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.padding(6.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, null, Modifier.size(14.dp), tint = EditorPrimary)
                            Spacer(Modifier.width(4.dp))
                            Text(name, color = EditorText, fontSize = 11.sp, maxLines = 1)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Close, null, Modifier.size(14.dp).clickable { imageBase64 = null; imageFileName = null }, tint = EditorTextDim)
                        }
                    }
                }
                attachedFilePath?.let { path ->
                    Surface(color = EditorSecondary.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.padding(6.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, null, Modifier.size(14.dp), tint = EditorSecondary)
                            Spacer(Modifier.width(4.dp))
                            Text(path.substringAfterLast("/"), color = EditorText, fontSize = 11.sp, maxLines = 1)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Close, null, Modifier.size(14.dp).clickable { attachedFilePath = null }, tint = EditorTextDim)
                        }
                    }
                }
            }
        }

        // Input row
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Image, "Attach Image", tint = EditorTextDim)
            }
            IconButton(onClick = { filePicker.launch("*/*") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.AttachFile, "Attach File", tint = EditorTextDim)
            }
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text("Ask AI...", color = EditorTextDim) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = EditorText, unfocusedTextColor = EditorText,
                    focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant,
                    cursorColor = EditorPrimary
                ), maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        onSendMessage(input, imageBase64, attachedFilePath)
                        input = ""; imageBase64 = null; imageFileName = null; attachedFilePath = null
                    }
                },
                enabled = !isLoading && input.isNotBlank()
            ) {
                Icon(Icons.Default.Send, "Send", tint = EditorPrimary)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Text(if (isUser) "You" else "AI", color = EditorTextDim, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = if (isUser) EditorPrimary.copy(alpha = 0.2f) else EditorSurfaceVariant, modifier = Modifier.widthIn(max = 300.dp)) {
            Column(Modifier.padding(10.dp)) {
                if (message.attachedFilePath != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, null, Modifier.size(12.dp), tint = EditorSecondary)
                        Spacer(Modifier.width(4.dp))
                        Text(message.attachedFilePath.substringAfterLast("/"), color = EditorSecondary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (message.imageBase64 != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null, Modifier.size(12.dp), tint = EditorPrimary)
                        Spacer(Modifier.width(4.dp))
                        Text("[Image attached]", color = EditorPrimary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(message.content, color = EditorText, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}
