package com.example.myandroidapp.ui.screens.community

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.myandroidapp.ui.screens.library.viewer.FileViewerScreen
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.util.ScannedFile
import com.example.myandroidapp.util.StudyBuddyFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.UUID

// ═══════════════════════════════════════════════════════
// ── Media Preview for Community Feed ──
// Detects media type from URL/filename and shows:
//   • Images → Coil AsyncImage thumbnail
//   • Videos → Video thumbnail + play overlay
//   • PDFs → PDF icon card with filename
//   • Other → Generic file card
// ═══════════════════════════════════════════════════════

/**
 * Determines file type from a URL or filename.
 */
fun getMediaType(url: String?, name: String?): String {
    val ext = (name ?: url ?: "")
        .substringAfterLast('.', "")
        .substringBefore('?') // strip query params
        .lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> "IMAGE"
        "mp4", "avi", "mkv", "mov", "webm", "3gp" -> "VIDEO"
        "pdf" -> "PDF"
        else -> "OTHER"
    }
}

@Composable
fun MediaPreviewCard(
    attachmentUri: String?,
    attachmentName: String?,
    modifier: Modifier = Modifier,
    onOpenViewer: ((ScannedFile) -> Unit)? = null
) {
    if (attachmentUri.isNullOrBlank()) return

    val mediaType = getMediaType(attachmentUri, attachmentName)
    val displayName = attachmentName ?: attachmentUri.substringAfterLast('/').substringBefore('?')
    val context = LocalContext.current

    // State for the cached local file (for viewer)
    var cachedFile by remember(attachmentUri) { mutableStateOf<File?>(null) }
    var isDownloading by remember(attachmentUri) { mutableStateOf(false) }
    var showViewer by remember { mutableStateOf(false) }

    // Full-screen viewer overlay
    if (showViewer && cachedFile != null) {
        val sf = ScannedFile(
            name = displayName,
            absolutePath = cachedFile!!.absolutePath,
            size = cachedFile!!.length(),
            lastModified = cachedFile!!.lastModified(),
            type = mediaType,
            subfolder = ""
        )
        // Use existing FileViewerScreen as a full-screen overlay
        FileViewerScreen(
            scannedFile = sf,
            onDismiss = { showViewer = false }
        )
        return // overlay takes over
    }

    val handleClick: () -> Unit = {
        if (cachedFile != null) {
            showViewer = true
        } else if (!isDownloading) {
            isDownloading = true
            // Download to temp cache
            downloadAttachment(context, attachmentUri, displayName, mediaType) { file ->
                cachedFile = file
                isDownloading = false
                if (file != null) {
                    showViewer = true
                } else {
                    Toast.makeText(context, "Could not open file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    when (mediaType) {
        "IMAGE" -> ImagePreviewCard(
            url = attachmentUri,
            name = displayName,
            modifier = modifier,
            onClick = handleClick,
            isDownloading = isDownloading
        )
        "VIDEO" -> VideoPreviewCard(
            url = attachmentUri,
            name = displayName,
            modifier = modifier,
            onClick = handleClick,
            isDownloading = isDownloading
        )
        "PDF" -> PdfPreviewCard(
            name = displayName,
            modifier = modifier,
            onClick = handleClick,
            isDownloading = isDownloading
        )
        else -> GenericFileCard(
            name = displayName,
            modifier = modifier,
            onClick = handleClick,
            isDownloading = isDownloading
        )
    }
}

// ─────────────────────────────────────────────────────────
// ── Image Preview ──
// ─────────────────────────────────────────────────────────

@Composable
private fun ImagePreviewCard(
    url: String,
    name: String,
    modifier: Modifier,
    onClick: () -> Unit,
    isDownloading: Boolean
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(SurfaceCard)
    ) {
        Box {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .memoryCacheKey(url)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        Modifier.fillMaxWidth().height(200.dp).background(NavyLight),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(32.dp))
                    }
                },
                error = {
                    Box(
                        Modifier.fillMaxWidth().height(120.dp).background(NavyLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.BrokenImage, null, tint = TextMuted, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("Could not load image", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 280.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            if (isDownloading) {
                DownloadingOverlay()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// ── Video Preview ──
// ─────────────────────────────────────────────────────────

@Composable
private fun VideoPreviewCard(
    url: String,
    name: String,
    modifier: Modifier,
    onClick: () -> Unit,
    isDownloading: Boolean
) {
    val context = LocalContext.current

    // Try to get video thumbnail
    var thumbnail by remember(url) { mutableStateOf<Bitmap?>(null) }
    var thumbnailLoaded by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(url, hashMapOf<String, String>())
                thumbnail = retriever.getFrameAtTime(1000000) // 1 second
                retriever.release()
            } catch (_: Exception) { }
            thumbnailLoaded = true
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(SurfaceCard)
    ) {
        Box(
            Modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background: thumbnail or dark placeholder
            if (thumbnail != null) {
                androidx.compose.foundation.Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = "Video thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(NavyMedium, NavyLight)),
                            RoundedCornerShape(14.dp)
                        )
                )
            }

            // Dark overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.35f), RoundedCornerShape(14.dp))
            )

            // Play button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(TealPrimary.copy(0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // File name label at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.6f))),
                        RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Videocam, null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = name,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isDownloading) {
                DownloadingOverlay()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// ── PDF Preview ──
// ─────────────────────────────────────────────────────────

@Composable
private fun PdfPreviewCard(
    name: String,
    modifier: Modifier,
    onClick: () -> Unit,
    isDownloading: Boolean
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(RedError.copy(0.2f), PinkAccent.copy(0.1f)))
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF icon box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(RedError.copy(0.15f), PinkAccent.copy(0.08f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PictureAsPdf, null, tint = RedError, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("PDF Document", fontSize = 11.sp, color = TextMuted)
            }
            if (isDownloading) {
                CircularProgressIndicator(
                    color = TealPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// ── Generic File Card ──
// ─────────────────────────────────────────────────────────

@Composable
private fun GenericFileCard(
    name: String,
    modifier: Modifier,
    onClick: () -> Unit,
    isDownloading: Boolean
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(SurfaceCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealPrimary.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AttachFile, null, tint = TealPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Attachment", fontSize = 11.sp, color = TextMuted)
            }
            if (isDownloading) {
                CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// ── Downloading overlay ──
// ─────────────────────────────────────────────────────────

@Composable
private fun DownloadingOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.5f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text("Opening…", fontSize = 12.sp, color = Color.White)
        }
    }
}

// ─────────────────────────────────────────────────────────
// ── File download helper (Firebase Storage URL → temp file)
// ─────────────────────────────────────────────────────────

private fun downloadAttachment(
    context: Context,
    url: String,
    fileName: String,
    mediaType: String,
    onResult: (File?) -> Unit
) {
    Thread {
        try {
            val cacheDir = File(context.cacheDir, "media_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // Create a unique temp file
            val ext = fileName.substringAfterLast('.', "tmp")
            val tempFile = File(cacheDir, "${UUID.randomUUID()}.$ext")

            // Download
            URL(url).openStream().use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(tempFile)
            }
        } catch (e: Exception) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(null)
            }
        }
    }.start()
}
