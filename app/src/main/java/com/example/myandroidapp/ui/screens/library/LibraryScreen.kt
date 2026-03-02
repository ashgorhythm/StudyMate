package com.example.myandroidapp.ui.screens.library

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.data.model.StudyFile
import com.example.myandroidapp.ui.screens.library.viewer.FileViewerScreen
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // In-app file viewer state
    var viewingFile by remember { mutableStateOf<StudyFile?>(null) }

    // Show in-app file viewer if a file is being viewed
    viewingFile?.let { file ->
        FileViewerScreen(
            file = file,
            onDismiss = { viewingFile = null }
        )
        return
    }

    // SAF File Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.processPickedFile(context, it) }
    }

    val adaptive = rememberAdaptiveInfo()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
    ) {
        Column(
            modifier = Modifier
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
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            "Toggle View",
                            tint = TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

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
            val cats = listOf("All", "PDFs", "Notes", "Images", "Videos")
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
            Spacer(Modifier.height(16.dp))

            // ── File Count ──
            Text(
                "${uiState.files.size} files",
                fontSize = 12.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))

            // ── Files Grid/List ──
            if (uiState.files.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.FolderOpen, null, tint = PurpleAccent, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No files yet", color = TextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap + to upload your first file", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(TealPrimary, NavyDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Upload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upload File", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
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
                                onFav = { viewModel.toggleFavorite(file.id, file.isFavorite) },
                                onDelete = { viewModel.deleteFile(file) },
                                onOpen = { viewingFile = file }
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
                                onFav = { viewModel.toggleFavorite(file.id, file.isFavorite) },
                                onDelete = { viewModel.deleteFile(file) },
                                onOpen = { viewingFile = file }
                            )
                        }
                    }
                }
            }
        }

        // ── FAB - Real File Picker ──
        FloatingActionButton(
            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = adaptive.horizontalPadding, bottom = if (adaptive.isTablet) 24.dp else 12.dp),
            containerColor = TealPrimary,
            contentColor = NavyDark,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Upload, "Upload File")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileCard(file: StudyFile, onFav: () -> Unit, onDelete: () -> Unit, onOpen: () -> Unit) {
    val tc = when (file.fileType.uppercase()) {
        "PDF" -> RedError; "NOTE" -> AmberAccent; "IMAGE" -> GreenSuccess; "VIDEO" -> PurpleAccent; else -> TextSecondary
    }
    val ti = when (file.fileType.uppercase()) {
        "PDF" -> Icons.Default.PictureAsPdf; "NOTE" -> Icons.Default.Description
        "IMAGE" -> Icons.Default.Image; "VIDEO" -> Icons.Default.VideoFile; else -> Icons.Default.InsertDriveFile
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RedError.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = RedError)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(SurfaceCard),
            border = BorderStroke(1.dp, tc.copy(0.15f))
        ) {
            Column(Modifier.padding(12.dp)) {
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
                Spacer(Modifier.height(10.dp))
                Text(
                    file.fileName, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                if (file.subject.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(file.subject, fontSize = 10.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatFileSize(file.fileSize), fontSize = 10.sp, color = TextMuted)
                    IconButton(onFav, Modifier.size(24.dp)) {
                        Icon(
                            if (file.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder, "Fav",
                            tint = if (file.isFavorite) AmberAccent else TextMuted, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileListItem(file: StudyFile, onFav: () -> Unit, onDelete: () -> Unit, onOpen: () -> Unit) {
    val tc = when (file.fileType.uppercase()) {
        "PDF" -> RedError; "NOTE" -> AmberAccent; "IMAGE" -> GreenSuccess; "VIDEO" -> PurpleAccent; else -> TextSecondary
    }
    val ti = when (file.fileType.uppercase()) {
        "PDF" -> Icons.Default.PictureAsPdf; "NOTE" -> Icons.Default.Description
        "IMAGE" -> Icons.Default.Image; "VIDEO" -> Icons.Default.VideoFile; else -> Icons.Default.InsertDriveFile
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RedError.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = RedError)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen() },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(SurfaceCard),
            border = BorderStroke(1.dp, tc.copy(0.1f))
        ) {
            Row(
                Modifier.padding(12.dp),
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
                    Text(file.fileName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(file.fileType, fontSize = 10.sp, color = tc, fontWeight = FontWeight.SemiBold)
                        if (file.subject.isNotBlank()) {
                            Text(" • ${file.subject}", fontSize = 10.sp, color = TextSecondary)
                        }
                        Text(" • ${formatFileSize(file.fileSize)}", fontSize = 10.sp, color = TextMuted)
                    }
                }
                IconButton(onFav, Modifier.size(32.dp)) {
                    Icon(
                        if (file.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder, "Fav",
                        tint = if (file.isFavorite) AmberAccent else TextMuted, modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1e9)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1e6)
        bytes >= 1_000 -> String.format("%.0f KB", bytes / 1e3)
        else -> "$bytes B"
    }
}
