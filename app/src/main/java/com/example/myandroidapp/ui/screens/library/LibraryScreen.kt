package com.example.myandroidapp.ui.screens.library

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.data.model.StudyFile
import com.example.myandroidapp.ui.theme.*

@Composable
fun LibraryScreen(viewModel: LibraryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 100.dp)
        ) {
            Text("My Library", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search files...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
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

            val cats = listOf("All", "PDFs", "Notes", "Images", "Videos")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cats.forEach { c ->
                    val sel = uiState.selectedCategory == c
                    FilterChip(sel, { viewModel.setCategory(c) }, { Text(c, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary.copy(0.2f), selectedLabelColor = TealPrimary,
                            containerColor = SurfaceCard, labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(selectedBorderColor = TealPrimary, borderColor = Color.Transparent, enabled = true, selected = sel)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (uiState.files.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, null, tint = PurpleAccent, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No files yet", color = TextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    Button({ viewModel.addSampleFiles() }, colors = ButtonDefaults.buttonColors(TealPrimary, NavyDark), shape = RoundedCornerShape(12.dp)) {
                        Text("Load Sample Files", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    items(uiState.files) { file -> FileCard(file) { viewModel.toggleFavorite(file.id, file.isFavorite) } }
                }
            }
        }

        FloatingActionButton({ viewModel.addSampleFiles() }, Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 110.dp), containerColor = TealPrimary, contentColor = NavyDark, shape = CircleShape) {
            Icon(Icons.Default.Upload, "Upload")
        }
    }
}

@Composable
private fun FileCard(file: StudyFile, onFav: () -> Unit) {
    val tc = when (file.fileType.uppercase()) { "PDF" -> RedError; "NOTE" -> AmberAccent; "IMAGE" -> GreenSuccess; "VIDEO" -> PurpleAccent; else -> TextSecondary }
    val ti = when (file.fileType.uppercase()) { "PDF" -> Icons.Default.PictureAsPdf; "NOTE" -> Icons.Default.Description; "IMAGE" -> Icons.Default.Image; "VIDEO" -> Icons.Default.VideoFile; else -> Icons.Default.InsertDriveFile }
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(SurfaceCard), border = BorderStroke(1.dp, tc.copy(0.15f))) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp)).background(tc.copy(0.1f)), Alignment.Center) {
                Icon(ti, null, tint = tc, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(file.fileName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(if (file.fileSize >= 1_000_000) String.format("%.1f MB", file.fileSize / 1e6) else String.format("%.0f KB", file.fileSize / 1e3), fontSize = 10.sp, color = TextMuted)
                IconButton(onFav, Modifier.size(24.dp)) {
                    Icon(if (file.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder, "Fav", tint = if (file.isFavorite) AmberAccent else TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
