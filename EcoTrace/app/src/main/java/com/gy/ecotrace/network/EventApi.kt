package com.gy.ecotrace.network

import com.gy.ecotrace.network.dto.DeletedResponse
import com.gy.ecotrace.network.dto.EventBounds
import com.gy.ecotrace.network.dto.EventGoal
import com.gy.ecotrace.network.dto.EventMembersPage
import com.gy.ecotrace.network.dto.EventScheduleItem
import com.gy.ecotrace.network.dto.EventZone
import com.gy.ecotrace.network.dto.EventsPage
import com.gy.ecotrace.network.dto.JoinedResponse
import com.gy.ecotrace.network.dto.LeftResponse
import com.gy.ecotrace.network.dto.RemoteEvent
import com.gy.ecotrace.network.dto.UpdatedResponse
import com.gy.ecotrace.network.dto.ValidateResponse
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

interface EventApi {

    @GET("events")
    suspend fun list(
        @Query("filter") filter: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("after") after: Long? = null,
        @Query("name") name: String? = null,
        @Query("sort") sort: String? = null,
        @Query("min_members") minMembers: Int? = null,
        @Query("max_members") maxMembers: Int? = null,
        @Query("has_map") hasMap: Boolean? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius_km") radiusKm: Double? = null
    ): ApiResponse<EventsPage>

    @GET("events/bounds")
    suspend fun bounds(): ApiResponse<EventBounds>

    @GET("events/{id}")
    suspend fun get(@Path("id") id: Long): ApiResponse<RemoteEvent>

    @GET("events/{id}/goals")
    suspend fun goals(@Path("id") id: Long): ApiResponse<List<EventGoal>>

    @GET("events/{id}/schedule")
    suspend fun schedule(@Path("id") id: Long): ApiResponse<List<EventScheduleItem>>

    @GET("events/{id}/zones")
    suspend fun zones(@Path("id") id: Long): ApiResponse<List<EventZone>>

    @Multipart
    @POST("events")
    suspend fun create(
        @Part("data") data: RequestBody,
        @Part image: MultipartBody.Part?
    ): ApiResponse<RemoteEvent>

    @Multipart
    @PATCH("events/{id}")
    suspend fun update(
        @Path("id") id: Long,
        @Part("data") data: RequestBody,
        @Part image: MultipartBody.Part?
    ): ApiResponse<RemoteEvent>

    @DELETE("events/{id}")
    suspend fun delete(@Path("id") id: Long): ApiResponse<DeletedResponse>

    @GET("events/{id}/members")
    suspend fun members(
        @Path("id") id: Long,
        @Query("username") username: String? = null,
        @Query("after") after: Long? = null
    ): ApiResponse<EventMembersPage>

    @GET("events/{id}/members/preview")
    suspend fun membersPreview(
        @Path("id") id: Long,
        @Query("limit") limit: Int = 5
    ): ApiResponse<EventMembersPage>

    @POST("events/{id}/members")
    suspend fun join(@Path("id") id: Long): ApiResponse<JoinedResponse>

    @DELETE("events/{id}/members/me")
    suspend fun leave(@Path("id") id: Long): ApiResponse<LeftResponse>

    @PATCH("events/{id}/members/{userId}")
    suspend fun setRole(
        @Path("id") id: Long,
        @Path("userId") userId: Long,
        @Body body: Map<String, String>
    ): ApiResponse<UpdatedResponse>

    @POST("events/{id}/members/{userId}/validate")
    suspend fun validate(
        @Path("id") id: Long,
        @Path("userId") userId: Long
    ): ApiResponse<ValidateResponse>
}
