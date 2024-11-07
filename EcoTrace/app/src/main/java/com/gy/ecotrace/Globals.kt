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
import com.google.gson.Gson
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
        return variablesMapStr[key] ?: ""
    }

    fun setString(key: String, value: String) {
        variablesMapStr[key] = value
    }

    fun getImgUrl(folder: String, id: String): String {
        return DatabaseMethods.ApplicationDatabaseMethods().getImageLink(folder, id)
    }

    fun initToolbarIconBack(toolbar: Toolbar, context: Context, color: Int = R.color.ok_green) {
        val backIcon = ContextCompat.getDrawable(context, R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(ContextCompat.getColor(context, color), PorterDuff.Mode.SRC_ATOP)
        toolbar.navigationIcon = backIcon
    }
    fun initToolbarIconBack(toolbar: androidx.appcompat.widget.Toolbar, context: Context, color: Int = R.color.ok_green) {
        val backIcon = ContextCompat.getDrawable(context, R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(ContextCompat.getColor(context, color), PorterDuff.Mode.SRC_ATOP)
        toolbar.setNavigationIcon(backIcon)
    }



    private fun currentTimeMillis() = System.currentTimeMillis()

    private fun saveToCache(type: String, data: String) {
        val prefs = getSharedPreferences("consts", MODE_PRIVATE)
        with(prefs.edit()) {
            putString(type, data)
            putLong("${type}_timestamp", currentTimeMillis())
            apply()
        }
    }

    fun isCacheExpired(type: String): Boolean {
        val prefs = getSharedPreferences("consts", MODE_PRIVATE)
        val timestamp = prefs.getLong("${type}_timestamp", 0)
        return currentTimeMillis() - timestamp > 30L * 24 * 60 * 60 * 1000
    }


    private var eventRoles: Array<String>? = null
    private var groupRoles: Array<String>? = null
    private var userRoles: Array<String>? = null

    private var usersFilters: Array<Pair<String, String>>? = null
    private var eventsFilters: Array<Pair<String, String>>? = null
    private var groupsFilters: Array<Pair<String, String>>? = null

    private var filtersColor: Array<Pair<String, String>>? = null

    fun constCache(type: String): String? {
        return try {
            getSharedPreferences("consts", MODE_PRIVATE)
                .getString(type, null)
        } catch (e: Exception) {
            null
        }
    }

    fun setEventRoles(data: Any) {
        eventRoles = data as Array<String>
        saveToCache("eventRoles", Gson().toJson(data))
    }
    fun getEventRoles(): Array<String> {
        return eventRoles!!
    }

    fun setGroupRoles(data: Any) {
        groupRoles = data as Array<String>
        saveToCache("groupRoles", Gson().toJson(data))
    }
    fun getGroupRoles(): Array<String> {
        return groupRoles!!
    }

    fun setUserRoles(data: Any) {
        userRoles = data as Array<String>
        saveToCache("userRoles", Gson().toJson(data))
    }
    fun getUserRoles(): Array<String> {
        return userRoles!!
    }


    fun setUserFilters(data: Any) {
        val parsedData = (data as Array<DatabaseMethods.DataClasses.ConstantMap>).map { Pair(it.name, it.description) }.toTypedArray()
        usersFilters = parsedData
        saveToCache("usersFilters", Gson().toJson(data))
    }
    fun getUserFilters(): Array<Pair<String, String>> {
        return usersFilters!!
    }

    fun setEventsFilters(data: Any) {
        val parsedData = (data as Array<DatabaseMethods.DataClasses.ConstantMap>).map { Pair(it.name, it.description) }.toTypedArray()
        eventsFilters = parsedData
        saveToCache("eventsFilters", Gson().toJson(data))
    }
    fun getEventsFilters(): Array<Pair<String, String>> {
        return eventsFilters!!
    }

    fun setGroupsFilters(data: Any) {
        val parsedData = (data as Array<DatabaseMethods.DataClasses.ConstantMap>).map { Pair(it.name, it.description) }.toTypedArray()
        groupsFilters = parsedData
        saveToCache("groupsFilters", Gson().toJson(data))
    }
    fun getGroupsFilters(): Array<Pair<String, String>> {
        return groupsFilters ?: arrayOf()
    }

    fun setFiltersColors(data: Any) {
        val parsedData = (data as Array<DatabaseMethods.DataClasses.ConstantMap>).map { Pair(it.name, it.description) }.toTypedArray()
        filtersColor = parsedData
        saveToCache("filtersColors", Gson().toJson(data))
    }
    fun getFiltersColors(): Array<Pair<String, String>> {
        return filtersColor!!
    }
}
