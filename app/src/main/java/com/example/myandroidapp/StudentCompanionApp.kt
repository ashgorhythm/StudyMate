package com.example.myandroidapp

import android.app.Application
import com.example.myandroidapp.data.firebase.FirebaseSocialService
import com.example.myandroidapp.data.local.AppDatabase
import com.example.myandroidapp.data.repository.StudyRepository
import com.example.myandroidapp.data.repository.CommunityRepository
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

    val communityRepository: CommunityRepository by lazy {
        CommunityRepository(database.communityDao())
    }

    val socialDao by lazy { database.socialDao() }

    val firebaseSocialService by lazy { FirebaseSocialService() }

    override fun onCreate() {
        super.onCreate()
        TaskReminderManager.createNotificationChannel(this)
    }
}
