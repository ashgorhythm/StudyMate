package com.example.myandroidapp.util

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Manages the StudyBuddy folder in the device's Documents directory.
 * Path: Documents/StudyBuddy/
 *
 * The app has full read/write access to this folder since it lives under
 * the public Documents directory and we use MANAGE_EXTERNAL_STORAGE or
 * simply app-scoped access via getExternalFilesDir fallback.
 */
object StudyBuddyFolder {

    private const val FOLDER_NAME = "StudyBuddy"

    /**
     * Returns the StudyBuddy folder under public Documents, creating it if needed.
     * Falls back to app-specific external files dir if Documents is unavailable.
     */
    fun getOrCreate(context: Context): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val folder = File(documentsDir, FOLDER_NAME)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        // Also create sample sub-folders for organization
        listOf("PDFs", "Images", "Videos").forEach {
            val sub = File(folder, it)
            if (!sub.exists()) sub.mkdirs()
        }
        return folder
    }

    /**
     * Scans the StudyBuddy folder recursively and returns all files.
     */
    fun scanFiles(context: Context): List<ScannedFile> {
        val root = getOrCreate(context)
        val results = mutableListOf<ScannedFile>()
        root.walkTopDown().forEach { file ->
            if (file.isFile && !file.name.startsWith(".")) {
                results.add(
                    ScannedFile(
                        name = file.name,
                        absolutePath = file.absolutePath,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        type = classifyFile(file.name),
                        subfolder = file.parentFile?.name?.takeIf { it != FOLDER_NAME } ?: ""
                    )
                )
            }
        }
        return results.sortedByDescending { it.lastModified }
    }

    /**
     * Classify file type from its extension.
     */
    fun classifyFile(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> "PDF"
            "doc", "docx", "txt", "md", "rtf", "odt" -> "OTHER"
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> "IMAGE"
            "mp4", "avi", "mkv", "mov", "webm", "3gp" -> "VIDEO"
            else -> "OTHER"
        }
    }

    /**
     * Rename a file within the StudyBuddy folder.
     * Returns the new File if successful, null otherwise.
     */
    fun renameFile(file: File, newName: String): File? {
        val target = File(file.parentFile, newName)
        return if (!target.exists() && file.renameTo(target)) target else null
    }

    /**
     * Delete a file from disk.
     */
    fun deleteFile(file: File): Boolean = file.exists() && file.delete()
}

data class ScannedFile(
    val name: String,
    val absolutePath: String,
    val size: Long,
    val lastModified: Long,
    val type: String,
    val subfolder: String
)

