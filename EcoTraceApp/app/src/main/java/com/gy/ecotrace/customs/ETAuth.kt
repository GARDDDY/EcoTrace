package com.gy.ecotrace.customs

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.provider.ContactsContract.Data
import android.util.Log
import com.google.gson.Gson
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.db.DatabaseMethods
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class ETAuth {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val client = OkHttpClient()

    private val USER_ID_KEY = "user_id"
    private val USER_TOKEN_KEY = "user_token"

    companion object {
        private var instance: ETAuth? = null

        fun initialize(context: Context) {
            if (instance == null) {
                instance = ETAuth().apply {
                    sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                    editor = sharedPreferences.edit()
                }
            }

            Log.wtf("ETAuth", "All done!")
        }

        fun getInstance(): ETAuth {
            return instance ?: throw IllegalStateException("ETAuth is not initialized. Call initialize() first.")
        }
    }

    fun applyUID(userId: String? = null) {
        editor.putString(USER_ID_KEY, userId)
        editor.apply()
    }

    fun getUID(): String {
        return sharedPreferences.getString(USER_ID_KEY, null) ?: ""
    }

    fun applyUserToken(token: String? = null) {
        editor.putString(USER_TOKEN_KEY, token)
        editor.apply()
    }

    private fun getUserToken(): String? {
        return sharedPreferences.getString(USER_TOKEN_KEY, null)
    }

    fun create(e: String, p: String, u: DatabaseMethods.UserDatabaseMethods.User, callback: (Boolean) -> Unit) {
        DatabaseMethods.Account().createAccount(e, p, u) {
            if (it[1] == null) {
                callback(false)
                return@createAccount
            }

            applyUID(it[0])
            applyUserToken(it[1])

            callback(true)

        }
    }

    fun changePassword(from: String, to: String, callback: (Boolean) -> Unit) {
        DatabaseMethods.Account().changePassword(from, to) {
            callback(it)
        }
    }

    fun checkEmail(e: String, callback: (Boolean) -> Unit) {
        DatabaseMethods.Account().checkEmail(e) {
            callback(it)
        }
    }
    fun changeEmail(from: String?, to: String, callback: (Boolean) -> Unit) {
        DatabaseMethods.Account().changeEmail(from, to) {
            callback(it)
        }
    }

    fun set(data: DatabaseMethods.UserDatabaseMethods.UserChange, callback: (Boolean) -> Unit) {
        DatabaseMethods.Account().setData(data) {
            callback(it)
        }
    }
    fun setRules(data: MutableMap<String, Int>, callback: (Boolean) -> Unit) {
        DatabaseMethods.Account().setRules(data) {
            callback(it)
        }
    }

    fun setAvatar(image: Bitmap, callback: (Boolean) -> Unit) {
        DatabaseMethods.Account().setAvatar(image) {
            callback(it)
        }
    }

    fun authtkn(callback: (String?) -> Unit) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.SERVER_API_URI}gat?tkn=${getUserToken()}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.wtf("bad response", response.code.toString())
                        callback(null)
                        return
                    }

                    if (response.body == null) {
                        Log.wtf("bad response", "null")
                        callback(null)
                        return
                    }

                    val responseBody = response.body?.string()

                    val data = Gson().fromJson(responseBody, Array<String?>::class.java)

                    callback(data[0])
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.d("bad response", e.message.toString())
                callback(null)
            }
        })
    }
}