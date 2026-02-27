package com.example.myandroidapp

import android.app.Application
import com.example.myandroidapp.data.local.AppDatabase
import com.example.myandroidapp.data.repository.StudyRepository

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
}
