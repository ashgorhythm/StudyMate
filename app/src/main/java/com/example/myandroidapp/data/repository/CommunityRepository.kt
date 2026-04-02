package com.example.myandroidapp.data.repository

import com.example.myandroidapp.data.local.CommunityDao
import com.example.myandroidapp.data.local.PostWithComments
import com.example.myandroidapp.data.model.CommunityCommentEntity
import com.example.myandroidapp.data.model.CommunityPostEntity
import kotlinx.coroutines.flow.Flow

class CommunityRepository(private val dao: CommunityDao) {
    fun observePosts(): Flow<List<PostWithComments>> = dao.observePosts()

    suspend fun addPost(entity: CommunityPostEntity): Long = dao.insertPost(entity)

    suspend fun updatePost(entity: CommunityPostEntity) = dao.updatePost(entity)

    suspend fun addComment(entity: CommunityCommentEntity) = dao.insertComment(entity)
}

