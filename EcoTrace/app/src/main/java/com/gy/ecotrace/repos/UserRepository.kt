package com.gy.ecotrace.repos

import com.google.gson.Gson
import com.gy.ecotrace.db.room.EcoTraceDatabase
import com.gy.ecotrace.db.room.toProfileEntity
import com.gy.ecotrace.db.room.toRemote
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.network.UserApi
import com.gy.ecotrace.network.apiCall
import com.gy.ecotrace.network.dto.FriendsPage
import com.gy.ecotrace.network.dto.FriendshipStatus
import com.gy.ecotrace.network.dto.GraphData
import com.gy.ecotrace.network.dto.PrivacyRequest
import com.gy.ecotrace.network.dto.ProfileEditRequest
import com.gy.ecotrace.network.dto.RemoteUser
import com.gy.ecotrace.network.dto.UserEventsPage
import com.gy.ecotrace.network.dto.UserGroupsPage
import com.gy.ecotrace.network.dto.UserSearchPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: UserApi,
    private val tokenStore: TokenStore,
    private val gson: Gson
) {
    private val dao get() = EcoTraceDatabase.getInstance().userProfileDao()

    fun observeCurrentUser(): Flow<RemoteUser?> =
        dao.observe(tokenStore.userId).map { it?.toRemote() }

    suspend fun refreshCurrentUser(): RemoteUser {
        val user = apiCall { api.getUser("me") }
        dao.upsert(user.toProfileEntity())
        return user
    }

    suspend fun cachedCurrentUser(): RemoteUser? = dao.get(tokenStore.userId)?.toRemote()

    suspend fun getUser(userId: Long): RemoteUser = apiCall { api.getUser(userId.toString()) }

    suspend fun searchUsers(
        query: String? = null,
        filter: String? = null,
        sort: String? = null,
        commonGroup: Boolean? = null,
        commonEvent: Boolean? = null,
        after: Long? = null
    ): UserSearchPage =
        apiCall { api.searchUsers(query, filter, sort, commonGroup, commonEvent, after) }

    suspend fun updateProfile(edit: ProfileEditRequest, avatarPng: ByteArray? = null): RemoteUser {
        val dataPart = gson.toJson(edit).toRequestBody("application/json".toMediaTypeOrNull())
        val imagePart = avatarPng?.let {
            MultipartBody.Part.createFormData(
                "image", "avatar.png", it.toRequestBody("image/png".toMediaTypeOrNull())
            )
        }
        val user = apiCall { api.updateProfile(dataPart, imagePart) }
        dao.upsert(user.toProfileEntity())
        return user
    }

    suspend fun updatePrivacy(privacy: PrivacyRequest): Boolean =
        apiCall { api.updatePrivacy(privacy) }.updated

    suspend fun getFriends(userId: Long, sort: String? = null, after: Long? = null): FriendsPage =
        apiCall { api.getFriends(userId.toString(), sort, after) }

    suspend fun getFriendship(userId: Long): FriendshipStatus =
        apiCall { api.getFriendship(userId.toString()) }

    suspend fun addFriend(userId: Long): Int =
        apiCall { api.addFriend(userId.toString()) }.code

    suspend fun removeFriend(userId: Long): Boolean =
        apiCall { api.removeFriend(userId.toString()) }.removed

    suspend fun getGroups(userId: Long, sort: String? = null, after: Long? = null): UserGroupsPage =
        apiCall { api.getGroups(userId.toString(), sort, after) }

    suspend fun getEvents(userId: Long, sort: String? = null, after: Long? = null): UserEventsPage =
        apiCall { api.getEvents(userId.toString(), sort, after) }

    suspend fun getGraph(userId: Long, time: Int? = null, excludeTypes: List<Int>? = null): GraphData =
        apiCall { api.getGraph(userId.toString(), time, excludeTypes?.takeIf { it.isNotEmpty() }?.joinToString(",")) }
}
