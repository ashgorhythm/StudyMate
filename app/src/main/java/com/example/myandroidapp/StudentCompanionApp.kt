package com.example.myandroidapp

import android.app.Application
import com.example.myandroidapp.data.local.AppDatabase
import com.example.myandroidapp.data.repository.StudyRepository
import com.example.myandroidapp.service.TaskReminderManager

class StudentCompanionApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    val repository: StudyRepository by lazy {
        StudyRepository(
            taskDao = database.studyTaskDao(),
            subjectDao = database.subjectDao(),
            sessionDao = database.studySessionDao(),
            fileDao = database.studyFileDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Create notification channel for task reminders (required for Android 8+)
        TaskReminderManager.createNotificationChannel(this)
    }
}
