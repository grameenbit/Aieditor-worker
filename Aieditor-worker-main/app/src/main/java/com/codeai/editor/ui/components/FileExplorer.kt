package com.codeai.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeai.editor.data.model.FileNode
import com.codeai.editor.ui.theme.*

@Composable
fun FileExplorer(
    rootNode: FileNode?,
    onFileClick: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorSurface)
    ) {
        Text(
            "EXPLORER",
            color = EditorTextDim,
            fontSize = 11.sp,
            modifier = Modifier.padding(12.dp, 8.dp)
        )
        if (rootNode != null) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { FileTreeNode(rootNode, 0, onFileClick, onDeleteFile) }
            }
        } else {
            Text(
                "No project opened",
                color = EditorTextDim,
                fontSize = 13.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun FileTreeNode(
    node: FileNode,
    depth: Int,
    onFileClick: (String) -> Unit,
    onDeleteFile: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(depth < 2) }
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (node.isDirectory) expanded = !expanded
                else onFileClick(node.path)
            }
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
        Text(
            node.name,
            color = EditorText,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Box {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Menu",
                tint = EditorTextDim,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { showMenu = true }
            )
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDeleteFile(node.path)
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrorRed) }
                )
            }
        }
    }

    if (expanded && node.isDirectory) {
        node.children.forEach { child ->
            FileTreeNode(child, depth + 1, onFileClick, onDeleteFile)
        }
    }
}
