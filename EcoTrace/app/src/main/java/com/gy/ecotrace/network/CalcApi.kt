package com.gy.ecotrace.network

import com.gy.ecotrace.network.dto.CalcEntriesRequest
import com.gy.ecotrace.network.dto.CalcEntryResult
import com.gy.ecotrace.network.dto.CalcSpecifyChart
import com.gy.ecotrace.network.dto.CalcSpecifyConfig
import com.gy.ecotrace.network.dto.CalcState
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CalcApi {

    @GET("calculators/{id}/state")
    suspend fun state(@Path("id") calculatorId: Int): ApiResponse<CalcState>

    @POST("calculators/{id}/entries")
    suspend fun submit(
        @Path("id") calculatorId: Int,
        @Body body: CalcEntriesRequest
    ): ApiResponse<CalcEntryResult>

    @GET("calculators/specify-config")
    suspend fun specifyConfig(): ApiResponse<CalcSpecifyConfig>

    @GET("calculators/{id}/specify")
    suspend fun specify(
        @Path("id") calculatorId: Int,
        @Query("question") question: Int
    ): ApiResponse<CalcSpecifyChart>

    @GET("calculators/{id}/advices")
    suspend fun advices(
        @Path("id") calculatorId: Int,
        @Query("question") question: Int
    ): ApiResponse<List<String>>
}
