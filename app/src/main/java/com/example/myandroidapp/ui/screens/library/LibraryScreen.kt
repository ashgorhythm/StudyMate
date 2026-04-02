package com.example.myandroidapp.ui.screens.library

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.ui.screens.library.viewer.FileViewerScreen
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import com.example.myandroidapp.util.ScannedFile
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Initialize folder on first composition
    LaunchedEffect(Unit) { viewModel.initFolder(context) }

    // In-app file viewer state
    var viewingFile by remember { mutableStateOf<ScannedFile?>(null) }

    // Show in-app file viewer
    viewingFile?.let { file ->
        FileViewerScreen(
            scannedFile = file,
            onDismiss = { viewingFile = null }
        )
        return
    }

    // Rename dialog state
    var renamingFile by remember { mutableStateOf<ScannedFile?>(null) }
    var renameText by remember { mutableStateOf("") }

    if (renamingFile != null) {
        AlertDialog(
            onDismissRequest = { renamingFile = null },
            containerColor = NavyMedium,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Rename File", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    placeholder = { Text("New file name", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.3f),
                        focusedContainerColor = NavyDark, unfocusedContainerColor = NavyDark,
                        cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val file = renamingFile ?: return@TextButton
                    val newName = renameText.trim()
                    if (newName.isNotBlank() && newName != file.name) {
                        viewModel.renameFile(context, file, newName)
                    }
                    renamingFile = null
                }) { Text("Rename", color = TealPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { renamingFile = null }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    // Delete confirmation state
    var deletingFile by remember { mutableStateOf<ScannedFile?>(null) }

    if (deletingFile != null) {
        AlertDialog(
            onDismissRequest = { deletingFile = null },
            containerColor = NavyMedium,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete File?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${deletingFile?.name}\" will be permanently deleted from your device.",
                    color = TextSecondary, fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deletingFile?.let { viewModel.deleteFile(context, it) }
                    deletingFile = null
                }) { Text("Delete", color = RedError, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { deletingFile = null }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    val adaptive = rememberAdaptiveInfo()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd))),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (adaptive.maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified)
                        Modifier.widthIn(max = adaptive.maxContentWidth)
                    else Modifier
                )
                .fillMaxSize()
                .padding(horizontal = adaptive.horizontalPadding)
                .padding(top = if (adaptive.isTablet) 24.dp else 48.dp, bottom = if (adaptive.isTablet) 16.dp else 100.dp)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Library", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row {
                    IconButton(onClick = { viewModel.refreshFiles(context) }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = TextSecondary)
                    }
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            "Toggle View",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // ── Folder Path Info ──
            if (uiState.folderPath.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(0.08f)),
                    border = BorderStroke(1.dp, TealPrimary.copy(0.15f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Documents/StudyBuddy",
                            fontSize = 12.sp, color = TealPrimary, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── Search Bar ──
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search files...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, "Clear", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.3f),
                    focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
                    cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))

            // ── Category Filter Chips ──
            val cats = listOf("All", "PDFs", "Images", "Videos")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cats.forEach { c ->
                    val sel = uiState.selectedCategory == c
                    FilterChip(sel, { viewModel.setCategory(c) }, { Text(c, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary.copy(0.2f), selectedLabelColor = TealPrimary,
                            containerColor = SurfaceCard, labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            selectedBorderColor = TealPrimary, borderColor = Color.Transparent,
                            enabled = true, selected = sel
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── File Count ──
            Text("${uiState.files.size} files", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))

            // ── Loading ──
            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                    CircularProgressIndicator(color = TealPrimary)
                }
            }
            // ── Empty State ──
            else if (uiState.files.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.FolderOpen, null, tint = PurpleAccent, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No files yet", color = TextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Place files in Documents/StudyBuddy",
                        color = TextSecondary, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Use sub-folders: PDFs, Images, Videos",
                        color = TextMuted, fontSize = 11.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = { viewModel.refreshFiles(context) },
                        border = BorderStroke(1.dp, TealPrimary.copy(0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp), tint = TealPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("Scan Again", color = TealPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            // ── Files Grid/List ──
            else {
                if (uiState.isGridView) {
                    LazyVerticalGrid(
                        GridCells.Fixed(adaptive.gridColumns),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.files) { file ->
                            FileCard(
                                file = file,
                                onOpen = { viewingFile = file },
                                onRename = { renamingFile = file; renameText = file.name },
                                onDelete = { deletingFile = file },
                                onOpenExternal = { openFileExternally(context, file) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        GridCells.Fixed(1),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.files) { file ->
                            FileListItem(
                                file = file,
                                onOpen = { viewingFile = file },
                                onRename = { renamingFile = file; renameText = file.name },
                                onDelete = { deletingFile = file },
                                onOpenExternal = { openFileExternally(context, file) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Open file externally with FileProvider ──
// ═══════════════════════════════════════════════════════

private fun openFileExternally(context: android.content.Context, file: ScannedFile) {
    try {
        val javaFile = File(file.absolutePath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", javaFile)
        val mimeType = when (file.type) {
            "PDF" -> "application/pdf"
            "IMAGE" -> "image/*"
            "VIDEO" -> "video/*"
            else -> "*/*"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}

// ═══════════════════════════════════════════════════════
// ── File Card (Grid View) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun FileCard(
    file: ScannedFile,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val tc = fileColor(file.type)
    val ti = fileIcon(file.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, tc.copy(0.15f))
    ) {
        Column(Modifier.padding(12.dp)) {
            // Top: icon area + 3-dot menu
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tc.copy(0.1f)),
                    Alignment.Center
                ) {
                    Icon(ti, null, tint = tc, modifier = Modifier.size(36.dp))
                }
                // 3-dot menu
                FileOptionsMenu(
                    modifier = Modifier.align(Alignment.TopEnd),
                    onRename = onRename,
                    onDelete = onDelete,
                    onOpenExternal = onOpenExternal
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                file.name, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            if (file.subfolder.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(file.subfolder, fontSize = 10.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text(formatFileSize(file.size), fontSize = 10.sp, color = TextMuted)
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── File List Item (List View) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun FileListItem(
    file: ScannedFile,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val tc = fileColor(file.type)
    val ti = fileIcon(file.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, tc.copy(0.1f))
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(tc.copy(0.1f)),
                Alignment.Center
            ) {
                Icon(ti, null, tint = tc, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(file.type, fontSize = 10.sp, color = tc, fontWeight = FontWeight.SemiBold)
                    if (file.subfolder.isNotBlank()) {
                        Text(" • ${file.subfolder}", fontSize = 10.sp, color = TextSecondary)
                    }
                    Text(" • ${formatFileSize(file.size)}", fontSize = 10.sp, color = TextMuted)
                }
            }
            // 3-dot menu
            FileOptionsMenu(
                modifier = Modifier,
                onRename = onRename,
                onDelete = onDelete,
                onOpenExternal = onOpenExternal
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── 3-Dot Options Menu ──
// ═══════════════════════════════════════════════════════

@Composable
private fun FileOptionsMenu(
    modifier: Modifier = Modifier,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onOpenExternal: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.MoreVert, "Options", tint = TextMuted, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = NavyMedium,
            shape = RoundedCornerShape(14.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Open with…", fontSize = 14.sp, color = TextPrimary) },
                onClick = { expanded = false; onOpenExternal() },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = TealPrimary, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Rename", fontSize = 14.sp, color = TextPrimary) },
                onClick = { expanded = false; onRename() },
                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, tint = PurpleAccent, modifier = Modifier.size(18.dp)) }
            )
            HorizontalDivider(color = TextMuted.copy(0.15f), modifier = Modifier.padding(horizontal = 12.dp))
            DropdownMenuItem(
                text = { Text("Delete", fontSize = 14.sp, color = RedError) },
                onClick = { expanded = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedError, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Helpers ──
// ═══════════════════════════════════════════════════════

private fun fileColor(type: String): Color = when (type) {
    "PDF" -> RedError; "IMAGE" -> GreenSuccess; "VIDEO" -> PurpleAccent; else -> TextSecondary
}

@Composable
private fun fileIcon(type: String) = when (type) {
    "PDF" -> Icons.Default.PictureAsPdf
    "IMAGE" -> Icons.Default.Image; "VIDEO" -> Icons.Default.VideoFile; else -> Icons.AutoMirrored.Filled.InsertDriveFile
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1e9)
        bytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", bytes / 1e6)
        bytes >= 1_000 -> String.format(Locale.US, "%.0f KB", bytes / 1e3)
        else -> "$bytes B"
    }
}
