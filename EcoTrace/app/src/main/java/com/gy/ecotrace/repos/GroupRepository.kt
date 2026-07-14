package com.gy.ecotrace.repos

import com.google.gson.Gson
import com.gy.ecotrace.db.room.EcoTraceDatabase
import com.gy.ecotrace.db.room.toEntity
import com.gy.ecotrace.db.room.toRemote
import com.gy.ecotrace.network.CommentEvent
import com.gy.ecotrace.network.GroupApi
import com.gy.ecotrace.network.PostEvent
import com.gy.ecotrace.network.RealtimeClient
import com.gy.ecotrace.network.apiCall
import com.gy.ecotrace.network.dto.CheckNameResponse
import com.gy.ecotrace.network.dto.CommentsPage
import com.gy.ecotrace.network.dto.CreateGroupRequest
import com.gy.ecotrace.network.dto.GroupComment
import com.gy.ecotrace.network.dto.GroupMembersPage
import com.gy.ecotrace.network.dto.GroupPost
import com.gy.ecotrace.network.dto.GroupsPage
import com.gy.ecotrace.network.dto.PostsPage
import com.gy.ecotrace.network.dto.RemoteGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val api: GroupApi,
    private val realtime: RealtimeClient,
    private val gson: Gson
) {
    private val dao get() = EcoTraceDatabase.getInstance().groupDao()

    fun observeGroup(groupId: Long): Flow<RemoteGroup?> =
        dao.observe(groupId).map { it?.toRemote() }

    suspend fun refreshGroup(groupId: Long): RemoteGroup {
        val group = apiCall { api.get(groupId) }
        dao.upsert(group.toEntity())
        return group
    }

    suspend fun cachedGroup(groupId: Long): RemoteGroup? = dao.get(groupId)?.toRemote()

    suspend fun posts(groupId: Long, before: Long? = null): PostsPage =
        apiCall { api.posts(groupId, before = before) }

    suspend fun newerPosts(groupId: Long, after: Long): PostsPage =
        apiCall { api.posts(groupId, after = after) }

    fun postEvents(groupId: Long): Flow<PostEvent> = realtime.groupPostEvents(groupId)

    suspend fun createPost(groupId: Long, text: String?, imagesPng: List<ByteArray> = emptyList()): GroupPost {
        val textPart = text?.toRequestBody("text/plain".toMediaTypeOrNull())
        val imageParts = imagesPng.mapIndexed { i, bytes ->
            MultipartBody.Part.createFormData("images", "post_$i.png", bytes.toRequestBody("image/png".toMediaTypeOrNull()))
        }
        return apiCall { api.createPost(groupId, textPart, imageParts) }
    }

    suspend fun deletePost(postId: Long): Boolean = apiCall { api.deletePost(postId) }.deleted

    suspend fun pinnedPost(groupId: Long): GroupPost? = apiCall { api.pinnedPost(groupId) }

    suspend fun post(postId: Long): GroupPost = apiCall { api.post(postId) }

    suspend fun pinPost(postId: Long): GroupPost = apiCall { api.pinPost(postId) }

    suspend fun unpinPost(postId: Long): Boolean = apiCall { api.unpinPost(postId) }.unpinned

    suspend fun comments(postId: Long, after: Long? = null): CommentsPage =
        apiCall { api.comments(postId, after) }

    fun commentEvents(postId: Long): Flow<CommentEvent> = realtime.commentEvents(postId)

    suspend fun createComment(postId: Long, text: String?, imagesPng: List<ByteArray> = emptyList()): GroupComment {
        val textPart = text?.toRequestBody("text/plain".toMediaTypeOrNull())
        val imageParts = imagesPng.mapIndexed { i, bytes ->
            MultipartBody.Part.createFormData("images", "comment_$i.png", bytes.toRequestBody("image/png".toMediaTypeOrNull()))
        }
        return apiCall { api.createComment(postId, textPart, imageParts) }
    }

    suspend fun deleteComment(commentId: Long): Boolean = apiCall { api.deleteComment(commentId) }.deleted

    suspend fun list(filter: String? = null, after: Long? = null, name: String? = null, sort: String? = null): GroupsPage =
        apiCall { api.list(filter, after, name, sort) }

    suspend fun checkName(name: String, excludeId: Long? = null): CheckNameResponse =
        apiCall { api.checkName(name, excludeId) }

    suspend fun create(request: CreateGroupRequest, imagePng: ByteArray? = null): RemoteGroup {
        val dataPart = gson.toJson(request).toRequestBody("application/json".toMediaTypeOrNull())
        val imagePart = imagePng?.let {
            MultipartBody.Part.createFormData("image", "group.png", it.toRequestBody("image/png".toMediaTypeOrNull()))
        }
        return apiCall { api.create(dataPart, imagePart) }
    }

    suspend fun update(groupId: Long, request: CreateGroupRequest, imagePng: ByteArray? = null): RemoteGroup {
        val dataPart = gson.toJson(request).toRequestBody("application/json".toMediaTypeOrNull())
        val imagePart = imagePng?.let {
            MultipartBody.Part.createFormData("image", "group.png", it.toRequestBody("image/png".toMediaTypeOrNull()))
        }
        val group = apiCall { api.update(groupId, dataPart, imagePart) }
        dao.upsert(group.toEntity())
        return group
    }

    suspend fun removeGroupImage(groupId: Long): RemoteGroup {
        val group = apiCall { api.removeImage(groupId) }
        dao.upsert(group.toEntity())
        return group
    }

    suspend fun delete(groupId: Long): Boolean {
        val ok = apiCall { api.delete(groupId) }.deleted
        if (ok) dao.delete(groupId)
        return ok
    }

    suspend fun join(groupId: Long): Boolean = apiCall { api.join(groupId) }.joined
    suspend fun leave(groupId: Long): Boolean = apiCall { api.leave(groupId) }.left

    suspend fun members(
        groupId: Long,
        role: String? = null,
        after: Long? = null,
        name: String? = null,
        sort: String? = null,
        limit: Int? = null
    ): GroupMembersPage = apiCall { api.members(groupId, role, after, name, sort, limit) }

    suspend fun setRole(groupId: Long, userId: Long, role: String): Boolean =
        apiCall { api.setRole(groupId, userId, mapOf("role" to role)) }.updated

    suspend fun kick(groupId: Long, userId: Long) {
        apiCall { api.kick(groupId, userId) }
    }
}
