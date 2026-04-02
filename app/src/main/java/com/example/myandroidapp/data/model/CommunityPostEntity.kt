package com.example.myandroidapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val author: String,
    val authorInitials: String,
    val timeAgoLabel: String,
    val title: String,
    val body: String,
    val tag: String,
    val upvotes: Int = 0,
    val isUpvoted: Boolean = false,
    val isDownvoted: Boolean = false,
    val attachmentPath: String? = null,
    val attachmentSize: Long? = null,
    val attachmentSubfolder: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

