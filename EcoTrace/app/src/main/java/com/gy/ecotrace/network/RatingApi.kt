package com.gy.ecotrace.network

import com.gy.ecotrace.network.dto.RatingPage
import retrofit2.http.GET
import retrofit2.http.Query

interface RatingApi {
    @GET("rating")
    suspend fun rating(
        @Query("country") country: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): ApiResponse<RatingPage>
}
