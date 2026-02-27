package com.example.myandroidapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val subjectName: String,
    val durationMinutes: Int,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0,
    val isCompleted: Boolean = false
)
