@file:Suppress("DEPRECATION")
package com.example.myandroidapp.service

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Drive backup service using the App Data folder (drive.appdata scope).
 *
 * Flow:
 *   1. User picks Google account via GoogleSignIn with DRIVE_APPDATA + DRIVE_FILE scopes
 *   2. GoogleAccountCredential uses the signed-in account to authenticate REST API calls
 *   3. Backup JSON is stored/updated in the hidden appDataFolder (invisible to user in Drive UI)
 *      — OR in the user's visible Drive if appDataFolder fails (fallback)
 */
class GoogleDriveService(private val context: Context, account: GoogleSignInAccount) {

    companion object {
        private const val TAG = "GoogleDriveService"
        private const val BACKUP_FILE_NAME = "studymate_backup.json"
        private const val MIME_JSON = "application/json"
    }

    private val credential: GoogleAccountCredential = GoogleAccountCredential.usingOAuth2(
        context, listOf(DriveScopes.DRIVE_APPDATA, DriveScopes.DRIVE_FILE)
    ).apply {
        selectedAccount = account.account
    }

    private val driveService: Drive = Drive.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    )
        .setApplicationName("StudyMate")
        .build()

    // ═══════════════════════════════════════════════════════
    // ── Upload Backup ──
    // ═══════════════════════════════════════════════════════

    /**
     * Uploads backup JSON to Drive's appDataFolder.
     * If a previous backup exists, it is overwritten (update).
     * Falls back to regular Drive space if appDataFolder isn't available.
     */
    suspend fun uploadBackup(jsonContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting backup upload (${jsonContent.length} bytes)...")

            // Try appDataFolder first, fall back to user-visible Drive
            val existingFileId = findBackupFile("appDataFolder") ?: findBackupFile("drive")

            val mediaContent = com.google.api.client.http.ByteArrayContent.fromString(
                MIME_JSON, jsonContent
            )

            if (existingFileId != null) {
                Log.d(TAG, "Updating existing backup file: $existingFileId")
                driveService.files().update(existingFileId, null, mediaContent).execute()
            } else {
                Log.d(TAG, "Creating new backup file in appDataFolder...")
                val fileMetadata = File().apply {
                    name = BACKUP_FILE_NAME
                    mimeType = MIME_JSON
                    parents = listOf("appDataFolder")
                }
                val created = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, parents")
                    .execute()
                Log.d(TAG, "Created backup: id=${created.id}, name=${created.name}")
            }

            Log.i(TAG, "Backup upload successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Backup upload FAILED", e)
            Result.failure(Exception("Drive upload failed: ${e.localizedMessage ?: e.javaClass.simpleName}", e))
        }
    }

    // ═══════════════════════════════════════════════════════
    // ── Download Backup ──
    // ═══════════════════════════════════════════════════════

    /**
     * Downloads the most recent backup from Drive.
     * Checks appDataFolder first, then falls back to user-visible Drive.
     */
    suspend fun downloadBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting backup download...")

            // Look in appDataFolder first, then regular drive
            val fileId = findBackupFile("appDataFolder") ?: findBackupFile("drive")

            if (fileId == null) {
                Log.w(TAG, "No backup file found in either appDataFolder or drive")
                return@withContext Result.failure(
                    Exception("No backup found in your Google Drive.\nMake sure you've created a backup first.")
                )
            }

            Log.d(TAG, "Downloading backup file: $fileId")
            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            val jsonContent = String(outputStream.toByteArray(), Charsets.UTF_8)
            Log.i(TAG, "Backup download successful (${jsonContent.length} bytes)")
            Result.success(jsonContent)
        } catch (e: Exception) {
            Log.e(TAG, "Backup download FAILED", e)
            Result.failure(Exception("Drive download failed: ${e.localizedMessage ?: e.javaClass.simpleName}", e))
        }
    }

    // ═══════════════════════════════════════════════════════
    // ── Helper: Find existing backup file ──
    // ═══════════════════════════════════════════════════════

    /**
     * Searches for the backup file in the given space.
     * @param space Either "appDataFolder" or "drive"
     * @return The file ID if found, null otherwise.
     */
    private fun findBackupFile(space: String): String? {
        return try {
            val result = driveService.files().list()
                .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
                .setSpaces(space)
                .setFields("files(id, name, modifiedTime)")
                .setPageSize(5)
                .execute()

            val fileId = result.files?.firstOrNull()?.id
            if (fileId != null) {
                Log.d(TAG, "Found backup in '$space': $fileId")
            }
            fileId
        } catch (e: Exception) {
            Log.w(TAG, "Could not search '$space': ${e.message}")
            null
        }
    }
}
