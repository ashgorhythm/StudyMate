@file:Suppress("DEPRECATION")
package com.example.myandroidapp.ui.screens.library.viewer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.util.ScannedFile
import java.io.File

// ══════════════════════════════════════════════════════════
// Enhanced Image Viewer
// Features:
//   • Pinch-to-zoom (0.5× – 6×)
//   • Double-tap to zoom to 2.5× / reset
//   • Pan / drag
//   • Rotation button (90° steps)
//   • Zoom level badge
//   • Open in another app
// ══════════════════════════════════════════════════════════

@Composable
fun ImageViewerScreen(
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

    // Transform state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var showZoomBadge by remember { mutableStateOf(false) }

    // Animate zoom badge visibility
    LaunchedEffect(scale) {
        showZoomBadge = true
        kotlinx.coroutines.delay(1200)
        showZoomBadge = false
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 6f)
        offset += panChange
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080810)),
        contentAlignment = Alignment.Center
    ) {
        // ── Image ──
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(fileUri)
                .crossfade(true)
                .build(),
            contentDescription = "Image viewer",
            contentScale = ContentScale.Fit,
            loading = {
                CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(48.dp))
            },
            error = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BrokenImage, null, tint = RedError, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Cannot display image", color = TextSecondary)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformState)
                .pointerInput(Unit) {
                    // Double-tap to zoom in / reset
                    detectTapGestures(
                        onDoubleTap = {
                            scale = if (scale < 1.5f) 2.5f else 1f
                            if (scale == 1f) offset = Offset.Zero
                        }
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    rotationZ = rotation
                }
        )

        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(listOf(Color(0xCC080810), Color(0x88080810), Color.Transparent))
                )
                .padding(horizontal = 8.dp, vertical = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(0.45f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    scannedFile.name, color = Color.White, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (scannedFile.subfolder.isNotBlank()) {
                    Text(scannedFile.subfolder, color = Color.White.copy(0.55f), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(6.dp))
            // Open in another app
            IconButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(fileUri, "image/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Open with…"))
                    } catch (_: Exception) {
                        Toast.makeText(context, "No app found to open this image", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(0.45f))
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open in app", tint = TealPrimary, modifier = Modifier.size(20.dp))
            }
        }

        // ── Bottom Toolbar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xBB080810)))
                )
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rotate CCW
            ImageToolButton(
                icon = Icons.Default.RotateLeft,
                label = "Rotate left",
                onClick = { rotation -= 90f }
            )
            Spacer(Modifier.width(16.dp))
            // Reset
            ImageToolButton(
                icon = Icons.Default.CenterFocusStrong,
                label = "Reset",
                onClick = { scale = 1f; offset = Offset.Zero; rotation = 0f }
            )
            Spacer(Modifier.width(16.dp))
            // Rotate CW
            ImageToolButton(
                icon = Icons.Default.RotateRight,
                label = "Rotate right",
                onClick = { rotation += 90f }
            )
        }

        // ── Zoom Level Badge ──
        if (showZoomBadge && scale != 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-90).dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(0.6f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    "${String.format("%.1f", scale)}×",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Hint (only at 1× zoom) ──
        if (scale == 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(0.45f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Pinch to zoom • Double-tap • Drag to pan", color = Color.White.copy(0.65f), fontSize = 11.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Toolbar button helper
// ─────────────────────────────────────────────────────────

@Composable
private fun ImageToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(0.5f))
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}
