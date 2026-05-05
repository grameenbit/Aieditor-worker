package com.codeai.editor.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeai.editor.data.model.FileNode
import com.codeai.editor.ui.theme.*

@Composable
fun FileExplorer(
    rootNode: FileNode?,
    onFileClick: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onRenameFile: (String, String) -> Unit,
    onMoveFile: (String, String) -> Unit,
    onCreateFile: (String) -> Unit,
    onCreateDir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(EditorSurface)) {
        Text("EXPLORER", color = EditorTextDim, fontSize = 11.sp, modifier = Modifier.padding(12.dp, 8.dp))
        if (rootNode != null) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    FileTreeNode(rootNode, 0, onFileClick, onDeleteFile, onRenameFile, onMoveFile, onCreateFile, onCreateDir)
                }
            }
        } else {
            Text("No project opened", color = EditorTextDim, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTreeNode(
    node: FileNode, depth: Int,
    onFileClick: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onRenameFile: (String, String) -> Unit,
    onMoveFile: (String, String) -> Unit,
    onCreateFile: (String) -> Unit,
    onCreateDir: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(depth < 2) }
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewDirDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(node.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false }, containerColor = EditorSurface,
            title = { Text("Rename", color = EditorText) },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary))
            },
            confirmButton = { Button(onClick = { onRenameFile(node.path, newName); showRenameDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)) { Text("Rename") } },
            dismissButton = { OutlinedButton(onClick = { showRenameDialog = false }) { Text("Cancel", color = EditorText) } }
        )
    }

    if (showMoveDialog) {
        var destDir by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMoveDialog = false }, containerColor = EditorSurface,
            title = { Text("Move to", color = EditorText) },
            text = {
                OutlinedTextField(value = destDir, onValueChange = { destDir = it },
                    placeholder = { Text("Destination folder path", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary))
            },
            confirmButton = { Button(onClick = { onMoveFile(node.path, destDir); showMoveDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)) { Text("Move") } },
            dismissButton = { OutlinedButton(onClick = { showMoveDialog = false }) { Text("Cancel", color = EditorText) } }
        )
    }

    if (showNewFileDialog) {
        var fileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false }, containerColor = EditorSurface,
            title = { Text("New File", color = EditorText) },
            text = {
                OutlinedTextField(value = fileName, onValueChange = { fileName = it },
                    placeholder = { Text("filename.kt", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary))
            },
            confirmButton = { Button(onClick = { onCreateFile("${node.path}/$fileName"); showNewFileDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)) { Text("Create") } },
            dismissButton = { OutlinedButton(onClick = { showNewFileDialog = false }) { Text("Cancel", color = EditorText) } }
        )
    }

    if (showNewDirDialog) {
        var dirName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDirDialog = false }, containerColor = EditorSurface,
            title = { Text("New Folder", color = EditorText) },
            text = {
                OutlinedTextField(value = dirName, onValueChange = { dirName = it },
                    placeholder = { Text("folder_name", color = EditorTextDim) }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = EditorText, unfocusedTextColor = EditorText, focusedBorderColor = EditorPrimary, unfocusedBorderColor = EditorSurfaceVariant, cursorColor = EditorPrimary))
            },
            confirmButton = { Button(onClick = { onCreateDir("${node.path}/$dirName"); showNewDirDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = EditorPrimary)) { Text("Create") } },
            dismissButton = { OutlinedButton(onClick = { showNewDirDialog = false }) { Text("Cancel", color = EditorText) } }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { if (node.isDirectory) expanded = !expanded else onFileClick(node.path) }, onLongClick = { showMenu = true })
            .padding(start = (depth * 16 + 8).dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                node.isDirectory && expanded -> Icons.Default.FolderOpen
                node.isDirectory -> Icons.Default.Folder
                node.name.endsWith(".kt") -> Icons.Default.Code
                node.name.endsWith(".xml") -> Icons.Default.Description
                else -> Icons.Default.InsertDriveFile
            },
            contentDescription = null,
            tint = when {
                node.isDirectory -> EditorSecondary
                node.name.endsWith(".kt") -> EditorPrimary
                else -> EditorTextDim
            },
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(node.name, color = EditorText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Box {
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDeleteFile(node.path) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrorRed) })
                DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; showRenameDialog = true }, leadingIcon = { Icon(Icons.Default.Edit, null, tint = EditorPrimary) })
                DropdownMenuItem(text = { Text("Copy Path") }, onClick = {
                    showMenu = false
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("path", node.path))
                }, leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = EditorText) })
                DropdownMenuItem(text = { Text("Move") }, onClick = { showMenu = false; showMoveDialog = true }, leadingIcon = { Icon(Icons.Default.DriveFileMove, null, tint = EditorText) })
                if (node.isDirectory) {
                    DropdownMenuItem(text = { Text("New File") }, onClick = { showMenu = false; showNewFileDialog = true }, leadingIcon = { Icon(Icons.Default.NoteAdd, null, tint = EditorSecondary) })
                    DropdownMenuItem(text = { Text("New Folder") }, onClick = { showMenu = false; showNewDirDialog = true }, leadingIcon = { Icon(Icons.Default.CreateNewFolder, null, tint = EditorSecondary) })
                }
            }
        }
    }

    if (expanded && node.isDirectory) {
        node.children.forEach { child ->
            FileTreeNode(child, depth + 1, onFileClick, onDeleteFile, onRenameFile, onMoveFile, onCreateFile, onCreateDir)
        }
    }
}
