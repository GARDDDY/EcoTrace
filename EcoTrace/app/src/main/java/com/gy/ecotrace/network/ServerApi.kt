package com.gy.ecotrace.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

interface ServerApi {
    // ResponseBody (not Unit) avoids the Gson converter touching the unparsed body.
    @GET("ping")
    suspend fun ping(): Response<ResponseBody>
}
