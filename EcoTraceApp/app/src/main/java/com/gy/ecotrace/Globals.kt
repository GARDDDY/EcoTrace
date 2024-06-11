package com.gy.ecotrace

import android.app.Application
import android.content.Context
import android.graphics.PorterDuff
import android.widget.Toolbar
import androidx.core.content.ContextCompat

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
        return if (variablesMapStr[key] != null) variablesMapStr[key]!! else "0"
    }

    fun setString(key: String, value: String) {
        variablesMapStr[key] = value
    }

    fun getImgUrl(folder: String, id: String): String {
        return "https://ik.imagekit.io/ecoimagetracekit/$folder/$id.png"
    }

    fun initToolbarIconBack(toolbar: Toolbar, context: Context) {
        val backIcon = ContextCompat.getDrawable(context, R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(ContextCompat.getColor(context, R.color.ok_green), PorterDuff.Mode.SRC_ATOP)
        toolbar.setNavigationIcon(backIcon)
    }
}
