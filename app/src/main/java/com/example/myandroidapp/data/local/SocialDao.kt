package com.example.myandroidapp.data.local

import androidx.room.*
import com.example.myandroidapp.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialDao {
    // ═══════ Communities ═══════

    @Query("SELECT * FROM communities ORDER BY memberCount DESC")
    fun observeAllCommunities(): Flow<List<CommunityEntity>>

    @Query("SELECT * FROM communities WHERE communityId = :communityId")
    suspend fun getCommunityById(communityId: String): CommunityEntity?

    @Query("SELECT * FROM communities WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchCommunities(query: String): Flow<List<CommunityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunity(community: CommunityEntity)

    @Update
    suspend fun updateCommunity(community: CommunityEntity)

    @Delete
    suspend fun deleteCommunity(community: CommunityEntity)

    // ═══════ Community Members ═══════

    @Query("SELECT * FROM community_members WHERE communityId = :communityId AND status = 'APPROVED'")
    fun observeApprovedMembers(communityId: String): Flow<List<CommunityMemberEntity>>

    @Query("SELECT * FROM community_members WHERE communityId = :communityId AND status = 'PENDING'")
    fun observePendingMembers(communityId: String): Flow<List<CommunityMemberEntity>>

    @Query("SELECT * FROM community_members WHERE memberId = :memberId AND status = 'APPROVED'")
    fun observeUserCommunities(memberId: String): Flow<List<CommunityMemberEntity>>

    @Query("SELECT * FROM community_members WHERE communityId = :communityId AND memberId = :memberId")
    suspend fun getMembership(communityId: String, memberId: String): CommunityMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: CommunityMemberEntity)

    @Update
    suspend fun updateMember(member: CommunityMemberEntity)

    @Query("DELETE FROM community_members WHERE communityId = :communityId AND memberId = :memberId")
    suspend fun removeMember(communityId: String, memberId: String)

    @Query("UPDATE communities SET memberCount = (SELECT COUNT(*) FROM community_members WHERE communityId = :communityId AND status = 'APPROVED') WHERE communityId = :communityId")
    suspend fun refreshMemberCount(communityId: String)

    // ═══════ User Profiles ═══════

    @Query("SELECT * FROM user_profiles WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUserProfile(): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE memberId = :memberId")
    suspend fun getProfileById(memberId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE memberId != :currentMemberId")
    fun observeOtherUsers(currentMemberId: String): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE displayName LIKE '%' || :query || '%'")
    fun searchUsers(query: String): Flow<List<UserProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    // ═══════ Friend Requests ═══════

    @Query("SELECT * FROM friend_requests WHERE toMemberId = :memberId AND status = 'PENDING'")
    fun observeIncomingRequests(memberId: String): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE fromMemberId = :memberId AND status = 'PENDING'")
    fun observeOutgoingRequests(memberId: String): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE ((fromMemberId = :memberId AND toMemberId = :otherId) OR (fromMemberId = :otherId AND toMemberId = :memberId))")
    suspend fun getFriendRequest(memberId: String, otherId: String): FriendRequestEntity?

    @Query("SELECT * FROM friend_requests WHERE ((fromMemberId = :memberId) OR (toMemberId = :memberId)) AND status = 'ACCEPTED'")
    fun observeAcceptedFriends(memberId: String): Flow<List<FriendRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendRequest(request: FriendRequestEntity)

    @Update
    suspend fun updateFriendRequest(request: FriendRequestEntity)

    @Delete
    suspend fun deleteFriendRequest(request: FriendRequestEntity)

    // ═══════ Chat Messages ═══════

    @Query("""SELECT * FROM chat_messages 
        WHERE (senderId = :userId AND receiverId = :otherUserId) 
        OR (senderId = :otherUserId AND receiverId = :userId) 
        ORDER BY sentAt ASC""")
    fun observeConversation(userId: String, otherUserId: String): Flow<List<ChatMessageEntity>>

    @Query("""SELECT * FROM chat_messages 
        WHERE senderId = :userId OR receiverId = :userId 
        ORDER BY sentAt DESC""")
    fun observeAllChats(userId: String): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET isRead = 1 WHERE receiverId = :userId AND senderId = :otherUserId AND isRead = 0")
    suspend fun markConversationRead(userId: String, otherUserId: String)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE receiverId = :userId AND isRead = 0")
    fun observeUnreadCount(userId: String): Flow<Int>
}
