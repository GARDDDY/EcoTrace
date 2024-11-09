package com.gy.ecotrace.customs

import android.content.Context
import android.graphics.Bitmap
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.network.dto.RegisterRequest
import com.gy.ecotrace.repos.AuthRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class ETAuth {

    private lateinit var tokenStore: TokenStore
    private lateinit var authRepository: AuthRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AuthBridge {
        fun authRepository(): AuthRepository
        fun tokenStore(): TokenStore
    }

    companion object {
        private var instance: ETAuth? = null

        fun initialize(context: Context) {
            if (instance == null) {
                val bridge = EntryPointAccessors.fromApplication(
                    context.applicationContext, AuthBridge::class.java
                )
                instance = ETAuth().apply {
                    tokenStore = bridge.tokenStore()
                    authRepository = bridge.authRepository()
                }
            }
        }

        fun getInstance(): ETAuth {
            return instance ?: throw IllegalStateException("ETAuth is not initialized. Call initialize() first.")
        }
    }

    fun getUID(): String = tokenStore.userId.takeIf { it > 0 }?.toString() ?: ""

    fun applyUID(userId: String? = null) {
        tokenStore.setUserId(userId?.toLongOrNull() ?: -1L)
    }

    fun applyUserToken(token: String? = null) {
        tokenStore.setToken(token)
    }

    fun create(e: String, p: String, u: DatabaseMethods.UserDatabaseMethods.User, callback: (Boolean) -> Unit) {
        scope.launch {
            val request = RegisterRequest(
                username = u.username,
                email = e,
                password = p,
                fullname = u.fullname?.takeIf { it.isNotBlank() },
                gender = genderToString(u.gender),
                aboutMe = u.about_me?.takeIf { it.isNotBlank() }
            )
            callback(runCatching { authRepository.register(request) }.isSuccess)
        }
    }

    fun logout(callback: (Boolean) -> Unit = {}) {
        scope.launch {
            authRepository.logout()
            callback(true)
        }
    }

    fun authtkn(callback: (String?) -> Unit) {
        callback(tokenStore.token)
    }

    fun changePassword(from: String, to: String, callback: (Boolean) -> Unit) {
        scope.launch {
            callback(runCatching { authRepository.changePassword(from, to) }.getOrDefault(false))
        }
    }

    fun changeEmail(email: String, password: String, callback: (Boolean) -> Unit) {
        scope.launch {
            callback(runCatching { authRepository.changeEmail(email, password) }.getOrDefault(false))
        }
    }

    fun set(data: DatabaseMethods.UserDatabaseMethods.UserChange, callback: (Boolean) -> Unit) {
        DatabaseMethods.Account().setData(data) { callback(it) }
    }

    fun setRules(data: MutableMap<String, Int>, callback: (Boolean) -> Unit) {
        DatabaseMethods.Account().setRules(data) { callback(it) }
    }

    fun setAvatar(image: Bitmap, callback: (Boolean) -> Unit) {
        DatabaseMethods.Account().setAvatar(image) { callback(it) }
    }

    private fun genderToString(gender: Int): String? = when (gender) {
        1 -> "male"
        2 -> "female"
        else -> null
    }
}
