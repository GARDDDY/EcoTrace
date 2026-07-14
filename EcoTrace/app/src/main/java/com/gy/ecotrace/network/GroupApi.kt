package com.gy.ecotrace.network

import com.gy.ecotrace.network.dto.CheckNameResponse
import com.gy.ecotrace.network.dto.CommentsPage
import com.gy.ecotrace.network.dto.DeletedResponse
import com.gy.ecotrace.network.dto.GroupComment
import com.gy.ecotrace.network.dto.GroupMembersPage
import com.gy.ecotrace.network.dto.GroupPost
import com.gy.ecotrace.network.dto.GroupsPage
import com.gy.ecotrace.network.dto.JoinedResponse
import com.gy.ecotrace.network.dto.LeftResponse
import com.gy.ecotrace.network.dto.PostsPage
import com.gy.ecotrace.network.dto.RemoteGroup
import com.gy.ecotrace.network.dto.UnpinnedResponse
import com.gy.ecotrace.network.dto.UpdatedResponse
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

interface GroupApi {

    @GET("groups")
    suspend fun list(
        @Query("filter") filter: String? = null,
        @Query("after") after: Long? = null,
        @Query("name") name: String? = null,
        @Query("sort") sort: String? = null
    ): ApiResponse<GroupsPage>

    @GET("groups/check-name")
    suspend fun checkName(
        @Query("name") name: String,
        @Query("excludeId") excludeId: Long? = null
    ): ApiResponse<CheckNameResponse>

    @GET("groups/{id}")
    suspend fun get(@Path("id") id: Long): ApiResponse<RemoteGroup>

    @Multipart
    @POST("groups")
    suspend fun create(
        @Part("data") data: RequestBody,
        @Part image: MultipartBody.Part?
    ): ApiResponse<RemoteGroup>

    @Multipart
    @PATCH("groups/{id}")
    suspend fun update(
        @Path("id") id: Long,
        @Part("data") data: RequestBody,
        @Part image: MultipartBody.Part?
    ): ApiResponse<RemoteGroup>

    @DELETE("groups/{id}")
    suspend fun delete(@Path("id") id: Long): ApiResponse<DeletedResponse>

    @DELETE("groups/{id}/image")
    suspend fun removeImage(@Path("id") id: Long): ApiResponse<RemoteGroup>

    @GET("groups/{id}/posts")
    suspend fun posts(
        @Path("id") id: Long,
        @Query("before") before: Long? = null,
        @Query("after") after: Long? = null
    ): ApiResponse<PostsPage>

    @Multipart
    @POST("groups/{id}/posts")
    suspend fun createPost(
        @Path("id") id: Long,
        @Part("text") text: RequestBody?,
        @Part images: List<MultipartBody.Part>
    ): ApiResponse<GroupPost>

    @DELETE("posts/{postId}")
    suspend fun deletePost(@Path("postId") postId: Long): ApiResponse<DeletedResponse>

    @GET("groups/{id}/posts/pinned")
    suspend fun pinnedPost(@Path("id") id: Long): ApiResponse<GroupPost?>

    @GET("posts/{postId}")
    suspend fun post(@Path("postId") postId: Long): ApiResponse<GroupPost>

    @POST("posts/{postId}/pin")
    suspend fun pinPost(@Path("postId") postId: Long): ApiResponse<GroupPost>

    @DELETE("posts/{postId}/pin")
    suspend fun unpinPost(@Path("postId") postId: Long): ApiResponse<UnpinnedResponse>

    @GET("posts/{postId}/comments")
    suspend fun comments(
        @Path("postId") postId: Long,
        @Query("after") after: Long? = null
    ): ApiResponse<CommentsPage>

    @Multipart
    @POST("posts/{postId}/comments")
    suspend fun createComment(
        @Path("postId") postId: Long,
        @Part("text") text: RequestBody?,
        @Part images: List<MultipartBody.Part>
    ): ApiResponse<GroupComment>

    @DELETE("comments/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: Long): ApiResponse<DeletedResponse>

    @GET("groups/{id}/members")
    suspend fun members(
        @Path("id") id: Long,
        @Query("role") role: String? = null,
        @Query("after") after: Long? = null,
        @Query("name") name: String? = null,
        @Query("sort") sort: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiResponse<GroupMembersPage>

    @POST("groups/{id}/members")
    suspend fun join(@Path("id") id: Long): ApiResponse<JoinedResponse>

    @DELETE("groups/{id}/members/me")
    suspend fun leave(@Path("id") id: Long): ApiResponse<LeftResponse>

    @PATCH("groups/{id}/members/{userId}")
    suspend fun setRole(
        @Path("id") id: Long,
        @Path("userId") userId: Long,
        @Body body: Map<String, String>
    ): ApiResponse<UpdatedResponse>

    @DELETE("groups/{id}/members/{userId}")
    suspend fun kick(
        @Path("id") id: Long,
        @Path("userId") userId: Long
    ): ApiResponse<DeletedResponse>
}
