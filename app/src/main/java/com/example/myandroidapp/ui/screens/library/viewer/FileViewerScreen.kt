package com.example.myandroidapp.ui.screens.library.viewer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.util.ScannedFile
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

/**
 * Full-screen in-app file viewer router.
 *
 *  PDF   → PdfViewerScreen   (continuous scroll, zoom)
 *  IMAGE → ImageViewerScreen (zoomable / pannable / rotatable via Coil)
 *  VIDEO → VideoPlayerScreen (ExoPlayer with full controls)
 *  OTHER → OtherFileViewerScreen (open-with button)
 */
@Composable
fun FileViewerScreen(
    scannedFile: ScannedFile,
    onDismiss: () -> Unit
) {
    when (scannedFile.type) {
        "PDF" -> PdfViewerScreen(scannedFile = scannedFile, onDismiss = onDismiss)
        "IMAGE" -> ImageViewerScreen(scannedFile = scannedFile, onDismiss = onDismiss)
        "VIDEO" -> VideoPlayerScreen(scannedFile = scannedFile, onDismiss = onDismiss)
        else -> OtherFileViewerScreen(scannedFile = scannedFile, onDismiss = onDismiss)
    }
}

// ═══════════════════════════════════════════════════════
// Video Player — powered by Media3 ExoPlayer
// Features:
//   • Play/Pause, seek bar, time display
//   • Tap to toggle controls
//   • Auto-hide controls after 3s
//   • Open in external app
// ═══════════════════════════════════════════════════════

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    scannedFile: ScannedFile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val javaFile = remember(scannedFile.absolutePath) { File(scannedFile.absolutePath) }
    val fileUri = remember(javaFile) {
        try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", javaFile)
        } catch (_: Exception) { Uri.fromFile(javaFile) }
    }

    // ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(fileUri))
            prepare()
            playWhenReady = true
        }
    }

    // Player state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Listen to player events
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                hasError = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Update position periodically
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(500L)
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls && isPlaying) {
            delay(3000L)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!hasError) {
            // ExoPlayer View
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // We use custom controls
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                }
            )

            // Tap to toggle controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showControls = !showControls }
            )

            // ── Custom Controls Overlay ──
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.35f))
                ) {
                    // ── Top Bar ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                            .padding(horizontal = 8.dp, vertical = 40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(0.4f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            scannedFile.name, color = Color.White, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(fileUri, "video/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open with…"))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "No app found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(0.4f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open externally", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // ── Center Play/Pause ──
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        IconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Replay10, "Rewind 10s", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        // Play/Pause
                        FloatingActionButton(
                            onClick = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                showControls = true
                            },
                            shape = CircleShape,
                            containerColor = TealPrimary,
                            contentColor = Color.White,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "Play/Pause",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = {
                                exoPlayer.seekTo(
                                    (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                )
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    // ── Bottom: Seek Bar + Time ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Time display
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(currentPosition), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(formatTime(duration), color = Color.White.copy(0.7f), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        // Seek bar
                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { fraction ->
                                exoPlayer.seekTo((fraction * duration).toLong())
                                currentPosition = (fraction * duration).toLong()
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = TealPrimary,
                                activeTrackColor = TealPrimary,
                                inactiveTrackColor = Color.White.copy(0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        } else {
            // Error state
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ErrorOutline, null, tint = RedError, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Cannot play this video", color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(fileUri, "video/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open with…"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "No app found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(TealPrimary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open with External App")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) { Text("Back", color = TextMuted) }
            }

            // Top bar even in error
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(horizontal = 8.dp, vertical = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(0.4f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%d:%02d", mins, secs)
    }
}

// ═══════════════════════════════════════════════════════
// Fallback for unsupported file types
// ═══════════════════════════════════════════════════════

@Composable
fun OtherFileViewerScreen(
    scannedFile: ScannedFile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val javaFile = remember(scannedFile.absolutePath) { File(scannedFile.absolutePath) }
    val fileUri = remember(javaFile) {
        try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", javaFile)
        } catch (_: Exception) { Uri.fromFile(javaFile) }
    }

    val fileColor = TextSecondary
    val fileIcon = Icons.AutoMirrored.Filled.InsertDriveFile

    Box(
        Modifier.fillMaxSize().background(Color(0xFF060915)),
        Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(fileColor.copy(0.12f)),
                Alignment.Center
            ) {
                Icon(fileIcon, null, tint = fileColor, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                scannedFile.name, color = TextPrimary, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "In-app preview not available for this file type",
                color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(fileUri, "*/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Open with…"))
                    } catch (_: Exception) {
                        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(TealPrimary, NavyDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(0.7f).height(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open with App", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss) {
                Text("Back", color = TextMuted)
            }
        }
    }
}
