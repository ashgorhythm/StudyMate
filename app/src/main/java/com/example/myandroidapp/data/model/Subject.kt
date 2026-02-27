package com.example.myandroidapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "📚", // emoji icon
    val colorHex: String = "#13ECEC",
    val totalTopics: Int = 0,
    val completedTopics: Int = 0,
    val totalStudyMinutes: Long = 0
)
