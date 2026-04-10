package com.example.myandroidapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.AppSpecificStorageConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Media compression utility for reducing upload sizes.
 *
 * - Images: Downscales to max 1920px width, re-encodes as JPEG at 80% quality
 * - Videos: Uses LightCompressor to transcode to 720p with reasonable bitrate
 */
object MediaCompressor {

    /**
     * Compress an image URI. Returns a new URI pointing to the compressed temp file.
     *
     * @param maxWidth  Maximum width in pixels (height scales proportionally)
     * @param quality   JPEG quality 0–100
     * @return Compressed image URI, or original if compression fails / not needed
     */
    suspend fun compressImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1920,
        quality: Int = 80
    ): Uri = withContext(Dispatchers.IO) {
        try {
            // 1. Decode bounds only
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight

            // Skip compression for small images
            if (origWidth <= maxWidth && origHeight <= maxWidth) return@withContext uri

            // 2. Calculate inSampleSize for efficient downscaling
            var sampleSize = 1
            var w = origWidth; var h = origHeight
            while (w / 2 >= maxWidth || h / 2 >= maxWidth) {
                w /= 2; h /= 2; sampleSize *= 2
            }

            // 3. Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return@withContext uri

            // 4. Further resize if still too large
            val scaleFactor = if (bitmap.width > maxWidth) {
                maxWidth.toFloat() / bitmap.width
            } else 1f

            val finalBitmap = if (scaleFactor < 1f) {
                val newW = (bitmap.width * scaleFactor).toInt()
                val newH = (bitmap.height * scaleFactor).toInt()
                Bitmap.createScaledBitmap(bitmap, newW, newH, true).also {
                    if (it !== bitmap) bitmap.recycle()
                }
            } else bitmap

            // 5. Write compressed JPEG to cache
            val cacheDir = File(context.cacheDir, "compressed_media").apply { mkdirs() }
            val outFile = File(cacheDir, "img_${UUID.randomUUID()}.jpg")

            FileOutputStream(outFile).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            }
            finalBitmap.recycle()

            Uri.fromFile(outFile)
        } catch (e: Exception) {
            uri // return original on failure
        }
    }

    /**
     * Compress a video URI using LightCompressor.
     * Targets 720p with medium quality for a good balance of size and quality.
     *
     * @param onProgress Optional progress callback (0.0 – 100.0)
     * @return Compressed video URI, or original if compression fails
     */
    suspend fun compressVideo(
        context: Context,
        uri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Uri = suspendCancellableCoroutine { cont ->
        VideoCompressor.start(
            context = context,
            uris = listOf(uri),
            isStreamable = true,
            storageConfiguration = AppSpecificStorageConfiguration(
                subFolderName = "compressed"
            ),
            configureWith = Configuration(
                quality = VideoQuality.MEDIUM,
                videoNames = listOf("vid_${UUID.randomUUID()}"),
                isMinBitrateCheckEnabled = false // Compress even small files
            ),
            listener = object : CompressionListener {
                override fun onProgress(index: Int, percent: Float) {
                    onProgress?.invoke(percent)
                }

                override fun onStart(index: Int) {
                    // Compression started
                }

                override fun onSuccess(index: Int, size: Long, path: String?) {
                    if (path != null) {
                        cont.resume(Uri.fromFile(File(path)))
                    } else {
                        cont.resume(uri) // fallback to original
                    }
                }

                override fun onFailure(index: Int, failureMessage: String) {
                    cont.resume(uri) // fallback to original on failure
                }

                override fun onCancelled(index: Int) {
                    cont.resume(uri)
                }
            }
        )
    }

    /**
     * Detect media type from file name or URI string.
     */
    fun getMediaTypeFromName(name: String?): String {
        val ext = (name ?: "").substringAfterLast('.', "").substringBefore('?').lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "IMAGE"
            "mp4", "avi", "mkv", "mov", "webm", "3gp" -> "VIDEO"
            "pdf" -> "PDF"
            else -> "OTHER"
        }
    }
}
