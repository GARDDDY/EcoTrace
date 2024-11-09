package com.gy.ecotrace.network

import com.gy.ecotrace.network.dto.FriendActionResponse
import com.gy.ecotrace.network.dto.FriendsPage
import com.gy.ecotrace.network.dto.FriendshipStatus
import com.gy.ecotrace.network.dto.GraphData
import com.gy.ecotrace.network.dto.PrivacyRequest
import com.gy.ecotrace.network.dto.RemoteUser
import com.gy.ecotrace.network.dto.UpdatedResponse
import com.gy.ecotrace.network.dto.UserEventsPage
import com.gy.ecotrace.network.dto.UserGroupsPage
import com.gy.ecotrace.network.dto.UserSearchPage
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApi {

    // id may be numeric or "me".
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): ApiResponse<RemoteUser>

    @GET("users")
    suspend fun searchUsers(
        @Query("q") q: String? = null,
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = null,
        @Query("common_group") commonGroup: Boolean? = null,
        @Query("common_event") commonEvent: Boolean? = null,
        @Query("after") after: Long? = null
    ): ApiResponse<UserSearchPage>

    @Multipart
    @PATCH("users/me")
    suspend fun updateProfile(
        @Part("data") data: RequestBody,
        @Part image: MultipartBody.Part?
    ): ApiResponse<RemoteUser>

    @PATCH("users/me/privacy")
    suspend fun updatePrivacy(@Body body: PrivacyRequest): ApiResponse<UpdatedResponse>

    @GET("users/{id}/friends")
    suspend fun getFriends(
        @Path("id") id: String,
        @Query("sort") sort: String? = null,
        @Query("after") after: Long? = null
    ): ApiResponse<FriendsPage>

    @GET("users/{id}/friendship")
    suspend fun getFriendship(@Path("id") id: String): ApiResponse<FriendshipStatus>

    @POST("users/{id}/friend")
    suspend fun addFriend(@Path("id") id: String): ApiResponse<FriendActionResponse>

    @DELETE("users/{id}/friend")
    suspend fun removeFriend(@Path("id") id: String): ApiResponse<FriendActionResponse>

    @GET("users/{id}/groups")
    suspend fun getGroups(
        @Path("id") id: String,
        @Query("sort") sort: String? = null,
        @Query("after") after: Long? = null
    ): ApiResponse<UserGroupsPage>

    @GET("users/{id}/events")
    suspend fun getEvents(
        @Path("id") id: String,
        @Query("sort") sort: String? = null,
        @Query("after") after: Long? = null
    ): ApiResponse<UserEventsPage>

    @GET("users/{id}/graph")
    suspend fun getGraph(
        @Path("id") id: String,
        @Query("time") time: Int? = null,
        @Query("types") types: String? = null
    ): ApiResponse<GraphData>
}
