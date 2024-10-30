package com.gy.ecotrace.customs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.gy.ecotrace.BuildConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

    fun auid(userId: String? = null) {
        editor.putString(USER_ID_KEY, userId)
        editor.apply()
    }

    fun guid(): String {
        return sharedPreferences.getString(USER_ID_KEY, null) ?: ""
    }

    fun ausertkn(token: String? = null) {
        editor.putString(USER_TOKEN_KEY, token)
        editor.apply()
    }

    private fun gusertkn(): String? {
        return sharedPreferences.getString(USER_TOKEN_KEY, null)
    }

    fun authtkn(callback: (String?) -> Unit) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.SERVER_API_URI}gat?tkn=${gusertkn()}")
            .build()

        Log.d("final url", request.url.toString())

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.d("response", "Not successful: ${response.code}")
                        callback(null)
                        return
                    }

                    if (response.body == null) {
                        Log.d("response", "Response body is null")
                        callback(null)
                        return
                    }

                    val responseBody = response.body?.string()

                    val data = Gson().fromJson(responseBody, Array<String?>::class.java)
//
                    Log.d("got req", "Response received")
                    callback(data[0])
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.d("bad request", "Request failed: ${e.message}")
                callback(null)
            }
        })
    }
}