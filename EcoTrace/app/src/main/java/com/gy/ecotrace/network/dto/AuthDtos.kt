package com.gy.ecotrace.network.dto

data class AuthData(
    val user: RemoteUser = RemoteUser(),
    val token: String = ""
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullname: String? = null,
    val gender: String? = null,           // "unknown" | "male" | "female"
    val aboutMe: String? = null
)

data class LoginRequest(
    val login: String,                    // email or username
    val password: String
)

data class EmailRequest(val email: String)

data class VerifyCodeRequest(
    val email: String,
    val code: String                      // 6 digits
)

data class ChangePasswordRequest(
    val email: String,
    val code: String,
    val password: String
)

data class UpdatePasswordRequest(
    val current: String,
    val next: String
)

data class UpdateEmailRequest(
    val email: String,
    val password: String
)

data class SentResponse(val sent: Boolean = false)
data class ValidResponse(val valid: Boolean = false)
data class ChangedResponse(val changed: Boolean = false)
data class LoggedOutResponse(val loggedOut: Boolean = false)
