package com.example.myandroidapp.data.firebase

import android.net.Uri
import com.example.myandroidapp.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firebase-backed service for all social features.
 * Replaces the Room-based SocialDao for online community data.
 */
class FirebaseSocialService {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Collection references
    private val communitiesRef = db.collection("communities")
    private val postsRef = db.collection("posts")
    private val commentsRef = db.collection("comments")
    private val membersRef = db.collection("community_members")
    private val profilesRef = db.collection("user_profiles")
    private val friendRequestsRef = db.collection("friend_requests")
    private val chatRef = db.collection("chat_messages")

    // ═══════ Auth ═══════

    val currentUserId: String? get() = auth.currentUser?.uid

    suspend fun ensureAuthenticated(): String {
        val user = auth.currentUser
        if (user != null) return user.uid
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: throw Exception("Auth failed")
    }

    suspend fun getOrCreateProfile(displayName: String): UserProfileEntity {
        val uid = ensureAuthenticated()
        val doc = profilesRef.document(uid).get().await()
        if (doc.exists()) {
            val existing = doc.toProfile()
            if (existing != null) return existing
        }
        val profile = UserProfileEntity(
            memberId = uid,
            displayName = displayName,
            isCurrentUser = true
        )
        profilesRef.document(uid).set(profile.toMap()).await()
        return profile
    }

    // ═══════ Communities ═══════

    fun observeCommunities(): Flow<List<CommunityEntity>> = callbackFlow {
        val reg = communitiesRef.orderBy("memberCount", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toCommunity() } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createCommunity(community: CommunityEntity) {
        communitiesRef.document(community.communityId).set(community.toMap()).await()
    }

    suspend fun getCommunity(communityId: String): CommunityEntity? {
        val doc = communitiesRef.document(communityId).get().await()
        return if (doc.exists()) doc.toCommunity() else null
    }

    // ═══════ Members ═══════

    suspend fun getMembership(communityId: String, memberId: String): CommunityMemberEntity? {
        val key = "${communityId}_${memberId}"
        val doc = membersRef.document(key).get().await()
        return if (doc.exists()) doc.toMember() else null
    }

    suspend fun insertMember(member: CommunityMemberEntity) {
        val key = "${member.communityId}_${member.memberId}"
        membersRef.document(key).set(member.toMap()).await()
        refreshMemberCount(member.communityId)
    }

    suspend fun updateMember(member: CommunityMemberEntity) {
        val key = "${member.communityId}_${member.memberId}"
        membersRef.document(key).set(member.toMap()).await()
        refreshMemberCount(member.communityId)
    }

    suspend fun removeMember(communityId: String, memberId: String) {
        val key = "${communityId}_${memberId}"
        membersRef.document(key).delete().await()
        refreshMemberCount(communityId)
    }

    fun observePendingMembers(communityId: String): Flow<List<CommunityMemberEntity>> = callbackFlow {
        val reg = membersRef
            .whereEqualTo("communityId", communityId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toMember() } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    private suspend fun refreshMemberCount(communityId: String) {
        val count = membersRef
            .whereEqualTo("communityId", communityId)
            .whereEqualTo("status", "APPROVED")
            .get().await().size()
        communitiesRef.document(communityId).update("memberCount", count).await()
    }

    // ═══════ Posts ═══════

    fun observePosts(): Flow<List<Map<String, Any>>> = callbackFlow {
        val reg = postsRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents?.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun addPost(post: CommunityPostEntity): String {
        val docId = postsRef.document().id
        val data = post.toMap().toMutableMap()
        data["firestoreId"] = docId
        postsRef.document(docId).set(data).await()
        return docId
    }

    suspend fun updatePost(firestoreId: String, updates: Map<String, Any>) {
        postsRef.document(firestoreId).update(updates).await()
    }

    // ═══════ Comments ═══════

    fun observeComments(postId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val reg = commentsRef
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents?.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun addComment(postId: String, author: String, authorInitials: String, body: String) {
        val data = mapOf(
            "postId" to postId,
            "author" to author,
            "authorInitials" to authorInitials,
            "body" to body,
            "timeAgoLabel" to "just now",
            "createdAt" to System.currentTimeMillis()
        )
        commentsRef.add(data).await()
    }

    // ═══════ Profiles ═══════

    suspend fun getProfile(memberId: String): UserProfileEntity? {
        val doc = profilesRef.document(memberId).get().await()
        return if (doc.exists()) doc.toProfile() else null
    }

    fun observeOtherUsers(currentId: String): Flow<List<UserProfileEntity>> = callbackFlow {
        val reg = profilesRef.addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val list = snap?.documents
                ?.mapNotNull { it.toProfile() }
                ?.filter { it.memberId != currentId }
                ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    // ═══════ Friend Requests ═══════

    fun observeIncomingRequests(memberId: String): Flow<List<FriendRequestEntity>> = callbackFlow {
        val reg = friendRequestsRef
            .whereEqualTo("toMemberId", memberId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { it.toFriendRequest() } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeAcceptedFriends(memberId: String): Flow<List<FriendRequestEntity>> = callbackFlow {
        // We need to listen to both directions
        var reg1: ListenerRegistration? = null
        var reg2: ListenerRegistration? = null
        val fromMe = mutableListOf<FriendRequestEntity>()
        val toMe = mutableListOf<FriendRequestEntity>()

        reg1 = friendRequestsRef
            .whereEqualTo("fromMemberId", memberId)
            .whereEqualTo("status", "ACCEPTED")
            .addSnapshotListener { snap, _ ->
                fromMe.clear()
                fromMe.addAll(snap?.documents?.mapNotNull { it.toFriendRequest() } ?: emptyList())
                trySend(fromMe + toMe)
            }
        reg2 = friendRequestsRef
            .whereEqualTo("toMemberId", memberId)
            .whereEqualTo("status", "ACCEPTED")
            .addSnapshotListener { snap, _ ->
                toMe.clear()
                toMe.addAll(snap?.documents?.mapNotNull { it.toFriendRequest() } ?: emptyList())
                trySend(fromMe + toMe)
            }
        awaitClose { reg1.remove(); reg2.remove() }
    }

    suspend fun sendFriendRequest(fromId: String, toId: String) {
        // Check existing
        val existing = friendRequestsRef
            .whereEqualTo("fromMemberId", fromId)
            .whereEqualTo("toMemberId", toId)
            .get().await()
        if (!existing.isEmpty) return
        val reverse = friendRequestsRef
            .whereEqualTo("fromMemberId", toId)
            .whereEqualTo("toMemberId", fromId)
            .get().await()
        if (!reverse.isEmpty) return

        val data = mapOf(
            "fromMemberId" to fromId,
            "toMemberId" to toId,
            "status" to "PENDING",
            "sentAt" to System.currentTimeMillis()
        )
        friendRequestsRef.add(data).await()
    }

    suspend fun updateFriendRequestStatus(docId: String, status: String) {
        friendRequestsRef.document(docId).update("status", status).await()
    }

    suspend fun deleteFriendRequest(docId: String) {
        friendRequestsRef.document(docId).delete().await()
    }

    // ═══════ Chat ═══════

    fun observeConversation(userId: String, otherUserId: String): Flow<List<ChatMessageEntity>> = callbackFlow {
        var reg1: ListenerRegistration? = null
        var reg2: ListenerRegistration? = null
        val sent = mutableListOf<ChatMessageEntity>()
        val received = mutableListOf<ChatMessageEntity>()

        reg1 = chatRef
            .whereEqualTo("senderId", userId)
            .whereEqualTo("receiverId", otherUserId)
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                sent.clear()
                sent.addAll(snap?.documents?.mapNotNull { it.toChatMessage() } ?: emptyList())
                trySend((sent + received).sortedBy { it.sentAt })
            }
        reg2 = chatRef
            .whereEqualTo("senderId", otherUserId)
            .whereEqualTo("receiverId", userId)
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                received.clear()
                received.addAll(snap?.documents?.mapNotNull { it.toChatMessage() } ?: emptyList())
                trySend((sent + received).sortedBy { it.sentAt })
            }
        awaitClose { reg1.remove(); reg2.remove() }
    }

    suspend fun sendMessage(senderId: String, receiverId: String, message: String) {
        val data = mapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "message" to message,
            "sentAt" to System.currentTimeMillis(),
            "isRead" to false
        )
        chatRef.add(data).await()
    }

    // ═══════ Storage ═══════

    suspend fun uploadAttachment(uri: Uri, fileName: String): String {
        val ref = storage.reference.child("attachments/${UUID.randomUUID()}_$fileName")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    // ═══════ Mapping Extensions ═══════

    private fun CommunityEntity.toMap() = mapOf(
        "communityId" to communityId, "name" to name, "description" to description,
        "isPublic" to isPublic, "creatorId" to creatorId,
        "memberCount" to memberCount, "createdAt" to createdAt, "iconEmoji" to iconEmoji
    )

    private fun CommunityMemberEntity.toMap() = mapOf(
        "communityId" to communityId, "memberId" to memberId,
        "displayName" to displayName, "role" to role.name,
        "status" to status.name, "joinedAt" to joinedAt
    )

    private fun UserProfileEntity.toMap() = mapOf(
        "memberId" to memberId, "displayName" to displayName,
        "bio" to bio, "studyStreak" to studyStreak,
        "totalStudyHours" to totalStudyHours, "isCurrentUser" to isCurrentUser,
        "createdAt" to createdAt
    )

    private fun CommunityPostEntity.toMap() = mapOf(
        "communityId" to communityId, "authorMemberId" to authorMemberId,
        "author" to author, "authorInitials" to authorInitials,
        "timeAgoLabel" to timeAgoLabel, "title" to title, "body" to body,
        "tag" to tag, "upvotes" to upvotes, "isUpvoted" to isUpvoted,
        "isDownvoted" to isDownvoted, "attachmentUri" to (attachmentUri ?: ""),
        "attachmentName" to (attachmentName ?: ""), "createdAt" to createdAt
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toCommunity(): CommunityEntity? {
        val d = data ?: return null
        return CommunityEntity(
            communityId = d["communityId"] as? String ?: id,
            name = d["name"] as? String ?: "",
            description = d["description"] as? String ?: "",
            isPublic = d["isPublic"] as? Boolean ?: true,
            creatorId = d["creatorId"] as? String ?: "",
            memberCount = (d["memberCount"] as? Number)?.toInt() ?: 0,
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            iconEmoji = d["iconEmoji"] as? String ?: "📚"
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toMember(): CommunityMemberEntity? {
        val d = data ?: return null
        return CommunityMemberEntity(
            communityId = d["communityId"] as? String ?: "",
            memberId = d["memberId"] as? String ?: "",
            displayName = d["displayName"] as? String ?: "",
            role = try { CommunityRole.valueOf(d["role"] as? String ?: "MEMBER") } catch (_: Exception) { CommunityRole.MEMBER },
            status = try { MembershipStatus.valueOf(d["status"] as? String ?: "APPROVED") } catch (_: Exception) { MembershipStatus.APPROVED },
            joinedAt = (d["joinedAt"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toProfile(): UserProfileEntity? {
        val d = data ?: return null
        return UserProfileEntity(
            memberId = d["memberId"] as? String ?: id,
            displayName = d["displayName"] as? String ?: "",
            bio = d["bio"] as? String ?: "",
            studyStreak = (d["studyStreak"] as? Number)?.toInt() ?: 0,
            totalStudyHours = (d["totalStudyHours"] as? Number)?.toInt() ?: 0,
            isCurrentUser = d["isCurrentUser"] as? Boolean ?: false,
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toFriendRequest(): FriendRequestEntity? {
        val d = data ?: return null
        return FriendRequestEntity(
            id = id.hashCode().toLong(),
            fromMemberId = d["fromMemberId"] as? String ?: "",
            toMemberId = d["toMemberId"] as? String ?: "",
            status = try { FriendRequestStatus.valueOf(d["status"] as? String ?: "PENDING") } catch (_: Exception) { FriendRequestStatus.PENDING },
            sentAt = (d["sentAt"] as? Number)?.toLong() ?: 0L
        ).also {
            // Store the Firestore doc ID for updates
            _docIdCache[it.id] = id
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toChatMessage(): ChatMessageEntity? {
        val d = data ?: return null
        return ChatMessageEntity(
            id = id.hashCode().toLong(),
            senderId = d["senderId"] as? String ?: "",
            receiverId = d["receiverId"] as? String ?: "",
            message = d["message"] as? String ?: "",
            sentAt = (d["sentAt"] as? Number)?.toLong() ?: 0L,
            isRead = d["isRead"] as? Boolean ?: false
        )
    }

    // Cache for mapping entity IDs back to Firestore doc IDs
    private val _docIdCache = mutableMapOf<Long, String>()
    fun getDocId(entityId: Long): String? = _docIdCache[entityId]
}
