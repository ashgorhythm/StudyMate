package com.example.myandroidapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "community_comments",
    foreignKeys = [
        ForeignKey(
            entity = CommunityPostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("postId")]
)
data class CommunityCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val author: String,
    val authorInitials: String,
    val body: String,
    val timeAgoLabel: String = "just now",
    val createdAt: Long = System.currentTimeMillis()
)

