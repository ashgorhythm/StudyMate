package com.example.myandroidapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: Long = System.currentTimeMillis(),
    val priority: Int = 0, // 0=low, 1=medium, 2=high
    val createdAt: Long = System.currentTimeMillis()
)
