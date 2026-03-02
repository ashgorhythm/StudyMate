package com.example.myandroidapp.ui.screens.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.model.StudyFile
import com.example.myandroidapp.data.repository.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LibraryUiState(
    val files: List<StudyFile> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isGridView: Boolean = true
)

class LibraryViewModel(private val repository: StudyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            repository.allFiles.collect { files ->
                _uiState.update { it.copy(files = files) }
            }
        }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        viewModelScope.launch {
            val typeForQuery = when (category) {
                "PDFs" -> "PDF"
                "Notes" -> "NOTE"
                "Images" -> "IMAGE"
                "Videos" -> "VIDEO"
                else -> null
            }
            val flow = if (typeForQuery == null) repository.allFiles
            else repository.getFilesByType(typeForQuery)
            flow.collect { files ->
                _uiState.update { it.copy(files = files) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.searchFiles(query).collect { files ->
                    _uiState.update { it.copy(files = files) }
                }
            }
        } else {
            loadFiles()
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun toggleFavorite(fileId: Long, currentFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFileFavorite(fileId, !currentFav)
        }
    }

    fun deleteFile(file: StudyFile) {
        viewModelScope.launch {
            repository.deleteFile(file)
        }
    }

    /**
     * Process a file selected via SAF (Storage Access Framework).
     * Extracts file metadata from the content URI and stores it in the database.
     */
    fun processPickedFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            val contentResolver = context.contentResolver

            var fileName = "Unknown File"
            var fileSize = 0L

            // Query the content resolver for file metadata
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: "Unknown File"
                    if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
                }
            }

            // Determine file type from extension
            val extension = fileName.substringAfterLast('.', "").lowercase()
            val fileType = when (extension) {
                "pdf" -> "PDF"
                "doc", "docx", "txt", "md", "rtf" -> "NOTE"
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> "IMAGE"
                "mp4", "avi", "mkv", "mov", "webm" -> "VIDEO"
                else -> "NOTE"
            }

            // Take persistent URI permission so the file can be accessed later
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Some providers don't support persistent permissions
            }

            val studyFile = StudyFile(
                fileName = fileName,
                filePath = uri.toString(),
                fileType = fileType,
                subject = "",
                fileSize = fileSize
            )
            repository.insertFile(studyFile)
        }
    }

}

class LibraryViewModelFactory(private val repository: StudyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
