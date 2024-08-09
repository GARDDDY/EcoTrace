package com.gy.ecotrace

import android.app.Application
import android.content.Context
import android.graphics.PorterDuff
import android.util.Log
import android.widget.Toast
import android.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.gy.ecotrace.db.DatabaseMethods
import java.time.LocalDateTime
import java.time.ZoneOffset

class Globals : Application() {
    private val variablesMapInt: HashMap<String, Int> = HashMap()
    private val variablesMapStr: HashMap<String, String> = HashMap()
    private val variablesMapBool: HashMap<String, Boolean> = HashMap()

    override fun onCreate() {
        super.onCreate()
        instance = this
        setDefaultValues()
    }

    companion object {
        private var instance: Globals? = null

        @JvmStatic
        fun applicationContext(): Context {
            return instance!!.applicationContext
        }

        @Synchronized
        fun getInstance(): Globals {
            if (instance == null) {
                throw IllegalStateException("Globals instance is null. It should have been initialized in onCreate()")
            }
            return instance!!
        }
    }

    private fun setDefaultValues() {
        setBool("profile-activity-is_registering", false)
    }

    fun getInt(key: String): Int {
        return if (variablesMapInt.containsKey(key)) {
            variablesMapInt[key]!!
        } else {
            0
        }
    }

    fun setInt(key: String, value: Int) {
        variablesMapInt[key] = value
    }

    fun getBool(key: String): Boolean {
        return if (variablesMapBool.containsKey(key)) {
            variablesMapBool[key]!!
        } else {
            false
        }
    }

    fun setBool(key: String, value: Boolean) {
        variablesMapBool[key] = value
    }

    fun getString(key: String): String {
        Log.d("Globals get value", "Got $key (${variablesMapStr[key] ?: '0'})")
        return variablesMapStr[key] ?: "0"
    }

    fun setString(key: String, value: String) {
        Log.d("Globals set value", "Set $key ${variablesMapStr[key]} -> $value")
        variablesMapStr[key] = value
    }

    fun getImgUrl(folder: String, id: String): String {
        return DatabaseMethods.ApplicationDatabaseMethods().getImageLink(folder, id)
    }

    fun initToolbarIconBack(toolbar: Toolbar, context: Context, color: Int = R.color.ok_green) {
        val backIcon = ContextCompat.getDrawable(context, R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(ContextCompat.getColor(context, color), PorterDuff.Mode.SRC_ATOP)
        toolbar.setNavigationIcon(backIcon)
    }
}
