package com.example.myandroidapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_files")
data class StudyFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileType: String, // "PDF", "NOTE", "IMAGE", "VIDEO"
    val subject: String = "",
    val fileSize: Long = 0,
    val isFavorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
