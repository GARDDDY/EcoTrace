package com.gy.ecotrace.repos

import com.gy.ecotrace.network.AuthApi
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.network.apiCall
import com.gy.ecotrace.network.dto.ChangePasswordRequest
import com.gy.ecotrace.network.dto.EmailRequest
import com.gy.ecotrace.network.dto.LoginRequest
import com.gy.ecotrace.network.dto.RegisterRequest
import com.gy.ecotrace.network.dto.RemoteUser
import com.gy.ecotrace.network.dto.UpdateEmailRequest
import com.gy.ecotrace.network.dto.UpdatePasswordRequest
import com.gy.ecotrace.network.dto.VerifyCodeRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore
) {
    val isLoggedIn: Boolean get() = tokenStore.isLoggedIn
    val currentUserId: Long get() = tokenStore.userId

    suspend fun register(request: RegisterRequest): RemoteUser {
        val auth = apiCall { api.register(request) }
        tokenStore.save(auth.token, auth.user.id)
        return auth.user
    }

    suspend fun login(login: String, password: String): RemoteUser {
        val auth = apiCall { api.login(LoginRequest(login, password)) }
        tokenStore.save(auth.token, auth.user.id)
        return auth.user
    }

    suspend fun logout() {
        runCatching { api.logout() }
        tokenStore.clear()
    }

    suspend fun me(): RemoteUser = apiCall { api.me() }

    suspend fun changePassword(current: String, next: String): Boolean =
        apiCall { api.updatePassword(UpdatePasswordRequest(current, next)) }.changed

    suspend fun changeEmail(email: String, password: String): Boolean =
        apiCall { api.updateEmail(UpdateEmailRequest(email, password)) }.changed

    suspend fun sendResetCode(email: String): Boolean =
        apiCall { api.sendResetCode(EmailRequest(email)) }.sent

    suspend fun verifyResetCode(email: String, code: String): Boolean =
        apiCall { api.verifyResetCode(VerifyCodeRequest(email, code)) }.valid

    suspend fun changePassword(email: String, code: String, password: String): Boolean =
        apiCall { api.changePassword(ChangePasswordRequest(email, code, password)) }.changed
}
