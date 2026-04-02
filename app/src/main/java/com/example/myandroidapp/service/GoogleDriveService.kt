package com.example.myandroidapp.service

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveService(private val context: Context, account: GoogleSignInAccount) {

    private val credential: GoogleAccountCredential = GoogleAccountCredential.usingOAuth2(
        context, listOf(DriveScopes.DRIVE_FILE)
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

    /**
     * Uploads the given JSON content as a file to Google Drive.
     * If a file named studymate_backup.json exists created by this app, it overrides it.
     */
    suspend fun uploadBackup(jsonContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check if file exists
            val fileList: FileList = driveService.files().list()
                .setQ("name = 'studymate_backup.json' and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val existingFileId = fileList.files.firstOrNull()?.id

            val fileMetadata = File().apply {
                name = "studymate_backup.json"
                mimeType = "application/json"
            }

            val mediaContent = com.google.api.client.http.ByteArrayContent.fromString(
                "application/json", jsonContent
            )

            if (existingFileId != null) {
                driveService.files().update(existingFileId, null, mediaContent).execute()
            } else {
                driveService.files().create(fileMetadata, mediaContent).execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Downloads the backup file from Google Drive and returns the JSON content.
     */
    suspend fun downloadBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileList: FileList = driveService.files().list()
                .setQ("name = 'studymate_backup.json' and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val existingFileId = fileList.files.firstOrNull()?.id
                ?: return@withContext Result.failure(Exception("No backup file found in Google Drive"))

            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(existingFileId).executeMediaAndDownloadTo(outputStream)

            val jsonContent = String(outputStream.toByteArray(), Charsets.UTF_8)
            Result.success(jsonContent)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
