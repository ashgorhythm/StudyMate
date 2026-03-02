package com.example.myandroidapp.ui.screens.library.viewer

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.myandroidapp.data.model.StudyFile
import com.example.myandroidapp.ui.theme.*

/**
 * Full-screen in-app file viewer.
 * - PDF  → WebView with Google Docs Viewer fallback
 * - IMAGE → Zoomable/pannable Compose image (Coil)
 * - VIDEO → Android VideoView with MediaController
 * - OTHER → Fallback to system intent
 */
@Composable
fun FileViewerScreen(
    file: StudyFile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val fileUri = remember(file.filePath) {
        if (file.filePath.startsWith("content://") || file.filePath.startsWith("file://"))
            Uri.parse(file.filePath)
        else
            null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060915))
    ) {
        when (file.fileType.uppercase()) {
            "PDF" -> PdfViewer(fileUri = fileUri, fileName = file.fileName)
            "IMAGE" -> ImageViewer(fileUri = fileUri)
            "VIDEO" -> VideoViewer(fileUri = fileUri)
            else -> FallbackViewer(
                file = file,
                onOpenExternal = {
                    fileUri?.let { uri ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    }
                }
            )
        }

        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xCC060915), Color.Transparent)
                    )
                )
                .padding(horizontal = 8.dp, vertical = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.4f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    file.fileName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.subject.isNotBlank()) {
                    Text(
                        file.subject,
                        fontSize = 12.sp,
                        color = Color.White.copy(0.6f)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            // Open external button
            IconButton(
                onClick = {
                    fileUri?.let { uri ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.4f))
            ) {
                Icon(Icons.Default.OpenInNew, "Open externally", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════
// PDF Viewer via Android WebView (Google Docs)
// ═══════════════════════════════════════════════

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PdfViewer(fileUri: Uri?, fileName: String) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (fileUri != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                isLoading = false
                                hasError = request?.isForMainFrame == true
                            }
                        }
                        // Load PDF via content URI directly
                        loadUrl(fileUri.toString())
                    }
                }
            )
        } else {
            hasError = true
        }

        // Loading indicator
        AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF060915)), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Loading PDF...", color = TextSecondary, fontSize = 14.sp)
                }
            }
        }

        // Error state
        AnimatedVisibility(visible = hasError && !isLoading, enter = fadeIn(), exit = fadeOut()) {
            PdfErrorView(
                onRetry = { hasError = false; isLoading = true }
            )
        }
    }
}

@Composable
private fun PdfErrorView(onRetry: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xFF060915)),
        Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, null, tint = RedError, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Unable to display PDF", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Try opening with an external PDF reader",
                color = TextSecondary, fontSize = 14.sp
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(TealPrimary, NavyDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

// ═══════════════════════════════════════════════
// Image Viewer — pinch-to-zoom + pan
// ═══════════════════════════════════════════════

@Composable
private fun ImageViewer(fileUri: Uri?) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 6f)
        offset += panChange
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF060915)),
        Alignment.Center
    ) {
        if (fileUri != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fileUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Image viewer",
                contentScale = ContentScale.Fit,
                loading = {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(40.dp))
                    }
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
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ImageNotSupported, null, tint = TextMuted, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(8.dp))
                Text("Image not available", color = TextSecondary)
            }
        }

        // Zoom hint
        if (scale == 1f) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Pinch to zoom • Drag to pan", color = Color.White.copy(0.7f), fontSize = 12.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════
// Video Viewer — native VideoView
// ═══════════════════════════════════════════════

@Composable
private fun VideoViewer(fileUri: Uri?) {
    var isReady by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
        Alignment.Center
    ) {
        if (fileUri != null && !hasError) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val mediaController = MediaController(ctx)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        setVideoURI(fileUri)
                        setOnPreparedListener { mp ->
                            isReady = true
                            mp.start()
                        }
                        setOnErrorListener { _, _, _ ->
                            hasError = true
                            true
                        }
                        requestFocus()
                        start()
                    }
                }
            )

            if (!isReady) {
                CircularProgressIndicator(color = PurpleAccent, modifier = Modifier.size(48.dp))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (hasError) Icons.Default.ErrorOutline else Icons.Default.VideocamOff,
                    null,
                    tint = if (hasError) RedError else TextMuted,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (hasError) "Cannot play this video" else "Video not available",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
// Fallback for other file types (DOC, TXT, etc.)
// ═══════════════════════════════════════════════

@Composable
private fun FallbackViewer(file: StudyFile, onOpenExternal: () -> Unit) {
    val fileColor = when (file.fileType.uppercase()) {
        "NOTE" -> AmberAccent
        else -> TextSecondary
    }
    val fileIcon = when (file.fileType.uppercase()) {
        "NOTE" -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }

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
                file.fileName,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "In-app preview not available for ${file.fileType} files",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onOpenExternal,
                colors = ButtonDefaults.buttonColors(TealPrimary, NavyDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(0.7f).height(52.dp)
            ) {
                Icon(Icons.Default.OpenInNew, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open with App", fontWeight = FontWeight.Bold)
            }
        }
    }
}
