package com.gy.ecotrace.network

import com.gy.ecotrace.network.dto.CalcAiSummaries
import com.gy.ecotrace.network.dto.CalcEntriesRequest
import com.gy.ecotrace.network.dto.CalcEntryResult
import com.gy.ecotrace.network.dto.CalcState
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CalcApi {

    @GET("calculators/{id}/state")
    suspend fun state(@Path("id") calculatorId: Int): ApiResponse<CalcState>

    @POST("calculators/{id}/entries")
    suspend fun submit(
        @Path("id") calculatorId: Int,
        @Body body: CalcEntriesRequest
    ): ApiResponse<CalcEntryResult>

    @GET("calculators/{id}/ai-summary")
    suspend fun aiSummary(@Path("id") calculatorId: Int): ApiResponse<CalcAiSummaries>
}
