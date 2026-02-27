package com.example.myandroidapp.ui.screens.library

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
            val flow = if (category == "All") repository.allFiles
            else repository.getFilesByType(category.uppercase())
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

    fun addSampleFiles() {
        viewModelScope.launch {
            val sampleFiles = listOf(
                StudyFile(fileName = "Calculus Notes Ch1-5.pdf", filePath = "/docs/calc.pdf", fileType = "PDF", subject = "Mathematics", fileSize = 2_500_000),
                StudyFile(fileName = "Physics Lab Report.pdf", filePath = "/docs/phys.pdf", fileType = "PDF", subject = "Physics", fileSize = 1_800_000),
                StudyFile(fileName = "Essay Draft.docx", filePath = "/docs/essay.docx", fileType = "NOTE", subject = "English", fileSize = 450_000),
                StudyFile(fileName = "History Timeline.png", filePath = "/docs/hist.png", fileType = "IMAGE", subject = "History", fileSize = 3_200_000),
                StudyFile(fileName = "Algebra Lecture.mp4", filePath = "/docs/algebra.mp4", fileType = "VIDEO", subject = "Mathematics", fileSize = 45_000_000),
                StudyFile(fileName = "Science Formulas.pdf", filePath = "/docs/formulas.pdf", fileType = "PDF", subject = "Physics", fileSize = 980_000)
            )
            sampleFiles.forEach { repository.insertFile(it) }
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
