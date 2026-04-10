package com.example.myandroidapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val communityId: String = "", // links post to a community
    val authorMemberId: String = "", // unique member id
    val author: String,
    val authorUsername: String = "", // @handle for the author
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
    val attachmentUri: String? = null, // for system file picker URIs
    val attachmentName: String? = null, // display name from file picker
    val createdAt: Long = System.currentTimeMillis()
)

