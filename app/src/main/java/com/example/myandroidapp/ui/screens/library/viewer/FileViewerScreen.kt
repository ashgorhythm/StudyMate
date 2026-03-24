package com.example.myandroidapp.ui.screens.library.viewer

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import android.widget.Toast
import androidx.compose.foundation.background
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
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.util.ScannedFile
import java.io.File

/**
 * Full-screen in-app file viewer router.
 *
 *  PDF   → PdfViewerScreen   (in-app rendered via PdfRenderer, with zoom & open-in-app)
 *  IMAGE → ImageViewerScreen (zoomable / pannable / rotatable via Coil)
 *  VIDEO → VideoViewer       (native VideoView + MediaController)
 *  OTHER → FallbackViewer    (open-with button)
 */
@Composable
fun FileViewerScreen(
    scannedFile: ScannedFile,
    onDismiss: () -> Unit
) {
    when (scannedFile.type) {
        "PDF" -> PdfViewerScreen(scannedFile = scannedFile, onDismiss = onDismiss)
        "IMAGE" -> ImageViewerScreen(scannedFile = scannedFile, onDismiss = onDismiss)
        "VIDEO" -> VideoFileViewerScreen(scannedFile = scannedFile, onDismiss = onDismiss)
        else -> OtherFileViewerScreen(scannedFile = scannedFile, onDismiss = onDismiss)
    }
}

// ═══════════════════════════════════════════════════════
// Video Viewer
// ═══════════════════════════════════════════════════════

@Composable
fun VideoFileViewerScreen(
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

    var isReady by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!hasError) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val mc = MediaController(ctx)
                        mc.setAnchorView(this)
                        setMediaController(mc)
                        setVideoURI(fileUri)
                        setOnPreparedListener { mp -> isReady = true; mp.start() }
                        setOnErrorListener { _, _, _ -> hasError = true; true }
                        requestFocus()
                        start()
                    }
                }
            )
            if (!isReady) {
                CircularProgressIndicator(
                    color = PurpleAccent,
                    modifier = Modifier.align(Alignment.Center).size(48.dp)
                )
            }
        } else {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ErrorOutline, null, tint = RedError, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Cannot play this video", color = TextSecondary, fontSize = 14.sp)
            }
        }

        // Top bar
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
                        Toast.makeText(context, "No app found to open this video", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(0.4f))
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open externally", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
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

    val fileColor = when (scannedFile.type) {
        "NOTE" -> AmberAccent
        else -> TextSecondary
    }
    val fileIcon = when (scannedFile.type) {
        "NOTE" -> Icons.Default.Description
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
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
                scannedFile.name, color = TextPrimary, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "In-app preview not available for ${scannedFile.type} files",
                color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    try {
                        val mimeType = if (scannedFile.type == "NOTE") "text/*" else "*/*"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(fileUri, mimeType)
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
