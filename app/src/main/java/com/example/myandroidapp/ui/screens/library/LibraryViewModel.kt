package com.example.myandroidapp.ui.screens.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.repository.StudyRepository
import com.example.myandroidapp.util.ScannedFile
import com.example.myandroidapp.util.StudyBuddyFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class LibraryUiState(
    val files: List<ScannedFile> = emptyList(),
    val allFiles: List<ScannedFile> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isGridView: Boolean = true,
    val folderPath: String = "",
    val isLoading: Boolean = false
)

class LibraryViewModel(private val repository: StudyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    /**
     * Initialize the StudyBuddy folder and scan its files.
     */
    fun initFolder(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val folder = withContext(Dispatchers.IO) { StudyBuddyFolder.getOrCreate(context) }
            _uiState.update { it.copy(folderPath = folder.absolutePath) }
            refreshFiles(context)
        }
    }

    /**
     * Rescan the StudyBuddy folder for file changes.
     */
    fun refreshFiles(context: Context) {
        viewModelScope.launch {
            val scanned = withContext(Dispatchers.IO) { StudyBuddyFolder.scanFiles(context) }
            _uiState.update { state ->
                val filtered = applyFilters(scanned, state.selectedCategory, state.searchQuery)
                state.copy(allFiles = scanned, files = filtered, isLoading = false)
            }
        }
    }

    fun setCategory(category: String) {
        _uiState.update { state ->
            val filtered = applyFilters(state.allFiles, category, state.searchQuery)
            state.copy(selectedCategory = category, files = filtered)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilters(state.allFiles, state.selectedCategory, query)
            state.copy(searchQuery = query, files = filtered)
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    /**
     * Delete a file from disk and refresh the list.
     */
    fun deleteFile(context: Context, file: ScannedFile) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                StudyBuddyFolder.deleteFile(File(file.absolutePath))
            }
            refreshFiles(context)
        }
    }

    /**
     * Rename a file on disk and refresh the list.
     */
    fun renameFile(context: Context, file: ScannedFile, newName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                StudyBuddyFolder.renameFile(File(file.absolutePath), newName)
            }
            refreshFiles(context)
        }
    }

    private fun applyFilters(
        files: List<ScannedFile>,
        category: String,
        query: String
    ): List<ScannedFile> {
        var result = files
        // Category filter
        if (category != "All") {
            val type = when (category) {
                "PDFs" -> "PDF"
                "Images" -> "IMAGE"
                "Videos" -> "VIDEO"
                else -> null
            }
            if (type != null) {
                result = result.filter { it.type == type }
            }
        }
        // Search filter
        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter {
                it.name.lowercase().contains(q) || it.subfolder.lowercase().contains(q)
            }
        }
        return result
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
