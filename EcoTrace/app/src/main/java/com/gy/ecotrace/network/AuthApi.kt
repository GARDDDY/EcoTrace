package com.gy.ecotrace.network

import com.gy.ecotrace.network.dto.AuthData
import com.gy.ecotrace.network.dto.ChangePasswordRequest
import com.gy.ecotrace.network.dto.ChangedResponse
import com.gy.ecotrace.network.dto.EmailRequest
import com.gy.ecotrace.network.dto.LoggedOutResponse
import com.gy.ecotrace.network.dto.LoginRequest
import com.gy.ecotrace.network.dto.RegisterRequest
import com.gy.ecotrace.network.dto.RemoteUser
import com.gy.ecotrace.network.dto.SentResponse
import com.gy.ecotrace.network.dto.UpdateEmailRequest
import com.gy.ecotrace.network.dto.UpdatePasswordRequest
import com.gy.ecotrace.network.dto.ValidResponse
import com.gy.ecotrace.network.dto.VerifyCodeRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {

    @POST("register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<AuthData>

    @POST("login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthData>

    @POST("logout")
    suspend fun logout(): ApiResponse<LoggedOutResponse>

    @GET("me")
    suspend fun me(): ApiResponse<RemoteUser>

    @POST("password/send-code")
    suspend fun sendResetCode(@Body body: EmailRequest): ApiResponse<SentResponse>

    @POST("password/verify-code")
    suspend fun verifyResetCode(@Body body: VerifyCodeRequest): ApiResponse<ValidResponse>

    @POST("password/change")
    suspend fun changePassword(@Body body: ChangePasswordRequest): ApiResponse<ChangedResponse>

    @PUT("users/me/password")
    suspend fun updatePassword(@Body body: UpdatePasswordRequest): ApiResponse<ChangedResponse>

    @PUT("users/me/email")
    suspend fun updateEmail(@Body body: UpdateEmailRequest): ApiResponse<ChangedResponse>
}
