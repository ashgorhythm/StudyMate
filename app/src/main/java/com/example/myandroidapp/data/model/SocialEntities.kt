package com.example.myandroidapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// ═══════════════════════════════════════════════════════
// ── Community Entity ──
// ═══════════════════════════════════════════════════════

@Entity(tableName = "communities")
data class CommunityEntity(
    @PrimaryKey val communityId: String = UUID.randomUUID().toString().take(8).uppercase(),
    val name: String,
    val description: String = "",
    val isPublic: Boolean = true, // false = private (needs approval)
    val creatorId: String, // memberId of who created it
    val memberCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val iconEmoji: String = "📚"
)

// ═══════════════════════════════════════════════════════
// ── Community Member ──
// ═══════════════════════════════════════════════════════

enum class CommunityRole { ADMIN, MODERATOR, MEMBER }
enum class MembershipStatus { APPROVED, PENDING }

@Entity(
    tableName = "community_members",
    primaryKeys = ["communityId", "memberId"]
)
data class CommunityMemberEntity(
    val communityId: String,
    val memberId: String, // unique user ID
    val displayName: String,
    val role: CommunityRole = CommunityRole.MEMBER,
    val status: MembershipStatus = MembershipStatus.APPROVED,
    val joinedAt: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════
// ── User Profile ──
// ═══════════════════════════════════════════════════════

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val memberId: String = UUID.randomUUID().toString().take(8).uppercase(),
    val displayName: String,
    val bio: String = "",
    val studyStreak: Int = 0,
    val totalStudyHours: Int = 0,
    val isCurrentUser: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════
// ── Friend Request ──
// ═══════════════════════════════════════════════════════

enum class FriendRequestStatus { PENDING, ACCEPTED, REJECTED }

@Entity(tableName = "friend_requests")
data class FriendRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromMemberId: String,
    val toMemberId: String,
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val sentAt: Long = System.currentTimeMillis()
)

// ═══════════════════════════════════════════════════════
// ── Chat Message ──
// ═══════════════════════════════════════════════════════

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: String,
    val receiverId: String,
    val message: String,
    val sentAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
