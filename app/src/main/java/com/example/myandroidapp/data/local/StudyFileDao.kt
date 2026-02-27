package com.example.myandroidapp.data.local

import androidx.room.*
import com.example.myandroidapp.data.model.StudyFile
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyFileDao {
    @Query("SELECT * FROM study_files ORDER BY addedAt DESC")
    fun getAllFiles(): Flow<List<StudyFile>>

    @Query("SELECT * FROM study_files WHERE fileType = :type ORDER BY addedAt DESC")
    fun getFilesByType(type: String): Flow<List<StudyFile>>

    @Query("SELECT * FROM study_files WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteFiles(): Flow<List<StudyFile>>

    @Query("SELECT * FROM study_files WHERE fileName LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%'")
    fun searchFiles(query: String): Flow<List<StudyFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: StudyFile): Long

    @Update
    suspend fun updateFile(file: StudyFile)

    @Delete
    suspend fun deleteFile(file: StudyFile)

    @Query("UPDATE study_files SET isFavorite = :fav WHERE id = :fileId")
    suspend fun toggleFavorite(fileId: Long, fav: Boolean)
}
