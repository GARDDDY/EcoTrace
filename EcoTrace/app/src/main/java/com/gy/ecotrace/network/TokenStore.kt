package com.gy.ecotrace.network

import android.content.Context
import androidx.core.content.edit

// Separate prefs file from legacy ETAuth ("app_preferences"), which stored user_id as a String.
class TokenStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val token: String? get() = prefs.getString(KEY_TOKEN, null)
    val userId: Long get() = prefs.getLong(KEY_USER_ID, -1L)
    val isLoggedIn: Boolean get() = token != null

    fun save(token: String, userId: Long) = prefs.edit {
        putString(KEY_TOKEN, token)
        putLong(KEY_USER_ID, userId)
    }

    fun setUserId(userId: Long) = prefs.edit { putLong(KEY_USER_ID, userId) }
    fun setToken(token: String?) = prefs.edit {
        if (token == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, token)
    }

    fun clear() = prefs.edit { clear() }

    companion object {
        private const val PREFS = "et_session"
        private const val KEY_TOKEN = "user_token"
        private const val KEY_USER_ID = "user_id"
    }
}
