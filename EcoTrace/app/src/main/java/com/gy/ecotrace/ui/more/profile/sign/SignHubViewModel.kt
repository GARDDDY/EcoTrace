package com.gy.ecotrace.ui.more.profile.sign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.network.dto.RegisterRequest
import com.gy.ecotrace.network.dto.RemoteUser
import com.gy.ecotrace.repos.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignHubViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var user = DatabaseMethods.UserDatabaseMethods.User()

    val currentUserId: Long get() = authRepository.currentUserId

    fun signIn(login: String, password: String, callback: (RemoteUser?) -> Unit) {
        viewModelScope.launch {
            callback(runCatching { authRepository.login(login, password) }.getOrNull())
        }
    }

    fun signUp(email: String, password: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val request = RegisterRequest(
                username = user.username,
                email = email,
                password = password,
                fullname = user.fullname?.takeIf { it.isNotBlank() },
                gender = genderToString(user.gender),
                aboutMe = user.about_me?.takeIf { it.isNotBlank() }
            )
            callback(runCatching { authRepository.register(request) }.isSuccess)
        }
    }

    fun sendForgotCode(email: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(runCatching { authRepository.sendResetCode(email) }.getOrDefault(false))
        }
    }

    fun checkCode(email: String, code: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(runCatching { authRepository.verifyResetCode(email, code) }.getOrDefault(false))
        }
    }

    fun applyPassword(email: String, code: String, password: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(runCatching { authRepository.changePassword(email, code, password) }.getOrDefault(false))
        }
    }

    // Легаси gender: 0/unknown шлём как null, сервер сам подставит 'unknown'.
    private fun genderToString(gender: Int): String? = when (gender) {
        1 -> "male"
        2 -> "female"
        else -> null
    }
}
