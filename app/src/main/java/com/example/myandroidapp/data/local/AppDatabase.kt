package com.example.myandroidapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myandroidapp.data.model.StudyFile
import com.example.myandroidapp.data.model.StudySession
import com.example.myandroidapp.data.model.StudyTask
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.data.model.CommunityPostEntity
import com.example.myandroidapp.data.model.CommunityCommentEntity
import com.example.myandroidapp.data.model.CommunityEntity
import com.example.myandroidapp.data.model.CommunityMemberEntity
import com.example.myandroidapp.data.model.UserProfileEntity
import com.example.myandroidapp.data.model.FriendRequestEntity
import com.example.myandroidapp.data.model.ChatMessageEntity

@Database(
    entities = [
        StudyTask::class, Subject::class, StudySession::class, StudyFile::class,
        CommunityPostEntity::class, CommunityCommentEntity::class,
        CommunityEntity::class, CommunityMemberEntity::class,
        UserProfileEntity::class, FriendRequestEntity::class, ChatMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun subjectDao(): SubjectDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun studyFileDao(): StudyFileDao
    abstract fun communityDao(): CommunityDao
    abstract fun socialDao(): SocialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_companion_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                     .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
