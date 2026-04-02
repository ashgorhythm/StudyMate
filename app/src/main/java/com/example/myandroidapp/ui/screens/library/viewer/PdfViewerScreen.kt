@file:Suppress("DEPRECATION")
package com.example.myandroidapp.ui.screens.library.viewer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.util.ScannedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ══════════════════════════════════════════════════════════
// Continuous In-App PDF Viewer using Android PdfRenderer
// Features:
//   • Smooth continuous scroll through all pages
//   • Global pinch-to-zoom & pan
//   • Page indicator (current / total)
//   • Open in external app button
// ══════════════════════════════════════════════════════════

@Composable
fun PdfViewerScreen(
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

    var totalPages by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val pages = remember { mutableStateMapOf<Int, Bitmap>() }
    // Store page aspect ratios for proper sizing
    val pageAspects = remember { mutableStateMapOf<Int, Float>() }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }

    val renderScale = 2f

    // Open PdfRenderer
    LaunchedEffect(javaFile) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(javaFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                pdfRenderer = renderer
                totalPages = renderer.pageCount
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMsg = "Cannot open PDF: ${e.localizedMessage}"
            }
        }
    }

    // Render a page on demand
    suspend fun renderPage(index: Int) {
        if (pages.containsKey(index)) return
        val renderer = pdfRenderer ?: return
        withContext(Dispatchers.IO) {
            try {
                val page = renderer.openPage(index)
                val width = (page.width * renderScale).toInt()
                val height = (page.height * renderScale).toInt()
                pageAspects[index] = page.width.toFloat() / page.height.toFloat()
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pages[index] = bmp
            } catch (_: Exception) { }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            pdfRenderer?.close()
            pages.values.forEach { it.recycle() }
            pages.clear()
        }
    }

    val listState = rememberLazyListState()

    // Track current page from scroll position
    LaunchedEffect(listState.firstVisibleItemIndex) {
        currentPage = listState.firstVisibleItemIndex + 1
    }

    // Global zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        when {
            isLoading -> {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Loading PDF…", color = TextSecondary, fontSize = 14.sp)
                }
            }
            errorMsg != null -> {
                Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = RedError, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(errorMsg ?: "Error", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(fileUri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "No PDF app found", Toast.LENGTH_SHORT).show()
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
            }
            else -> {
                // ── Continuous Pages with global zoom ──
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 96.dp, bottom = 24.dp)
                        .then(
                            if (scale > 1f) {
                                Modifier.transformable(transformState)
                            } else {
                                Modifier.transformable(transformState)
                            }
                        )
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(totalPages) { pageIndex ->
                            // Trigger render
                            LaunchedEffect(pageIndex) { renderPage(pageIndex) }

                            val bmp = pages[pageIndex]
                            val aspect = pageAspects[pageIndex] ?: (8.5f / 11f) // default letter aspect

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspect)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Page ${pageIndex + 1}",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        color = TealPrimary,
                                        modifier = Modifier.size(32.dp).align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Zoom Reset Button ──
                if (scale > 1.05f) {
                    FloatingActionButton(
                        onClick = { scale = 1f; offsetX = 0f; offsetY = 0f },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 80.dp)
                            .size(44.dp),
                        shape = CircleShape,
                        containerColor = NavyMedium.copy(0.9f),
                        contentColor = TealPrimary
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, "Reset zoom", modifier = Modifier.size(22.dp))
                    }
                }

                // ── Page Counter Pill ──
                if (totalPages > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(0.65f))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Page $currentPage / $totalPages",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ── Top Bar (overlaid) ──
        PdfTopBar(
            title = scannedFile.name,
            onBack = onDismiss,
            onOpenExternal = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────

@Composable
private fun PdfTopBar(title: String, onBack: () -> Unit, onOpenExternal: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xDD060915), Color(0xAA060915), Color.Transparent))
            )
            .padding(horizontal = 8.dp, vertical = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(0.4f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title, color = Color.White, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text("PDF Document", color = Color.White.copy(0.55f), fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onOpenExternal,
            modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(0.4f))
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open in app", tint = TealPrimary)
        }
    }
}
