package com.example.myandroidapp.data.local

import androidx.room.*
import com.example.myandroidapp.data.model.CommunityCommentEntity
import com.example.myandroidapp.data.model.CommunityPostEntity
import kotlinx.coroutines.flow.Flow

data class PostWithComments(
    @Embedded val post: CommunityPostEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "postId",
        entity = CommunityCommentEntity::class
    )
    val comments: List<CommunityCommentEntity>
)

@Dao
interface CommunityDao {
    @Transaction
    @Query("SELECT * FROM community_posts ORDER BY createdAt DESC")
    fun observePosts(): Flow<List<PostWithComments>>

    @Insert
    suspend fun insertPost(entity: CommunityPostEntity): Long

    @Update
    suspend fun updatePost(entity: CommunityPostEntity)

    @Insert
    suspend fun insertComment(entity: CommunityCommentEntity): Long

    @Query("DELETE FROM community_posts")
    suspend fun clearAll()
}

