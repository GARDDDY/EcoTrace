package com.gy.ecotrace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.databinding.ActivityMainBinding
import com.gy.ecotrace.db.DatabaseMethods
import com.yandex.mapkit.MapKitFactory
import com.yandex.maps.mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Properties


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ETAuth.initialize(this)

        // todo EDUCATION status
        // todo bugfixes, apply CHANGE_PROFILE, CREATE_PROFILE

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar : Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_news, R.id.navigation_education, R.id.navigation_ratings, R.id.navigation_more
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        val loggedUser = ETAuth.getInstance().guid()
        Globals.getInstance().setString("CurrentlyWatching", loggedUser ?: "0")

        startService(Intent(this, AppService::class.java))

        MapKitFactory.setApiKey(com.gy.ecotrace.BuildConfig.MAPKIT_API_KEY)


        fun applyData(type: Int, data1: String) {
            Log.d("data got, parsing", data1.toString())
            val data: Any = try {
                Gson().fromJson(data1, Array<String>::class.java)
            } catch (e: Exception) {
                try {
                    val jsonObject = Gson().fromJson(data1, Array<DatabaseMethods.DataClasses.ConstantMap>::class.java)
                    jsonObject
                } catch (e: Exception) {
                    Log.e("unable to unparse", data1.toString(), e)
                }
            }
            Log.d("data parsed, saving", data.toString())

            when (type) {
                0 -> Globals.getInstance().setEventRoles(data)
                1 -> Globals.getInstance().setGroupRoles(data)
                2 -> Globals.getInstance().setUserRoles(data)

                3 -> Globals.getInstance().setUserFilters(data)
                4 -> Globals.getInstance().setEventsFilters(data)
                5 -> Globals.getInstance().setGroupsFilters(data)
                6 -> Globals.getInstance().setFiltersColors(data)
            }
        }

        val inCache = arrayOf("eventRoles", "groupRoles", "userRoles", "usersFilters", "eventsFilters", "groupsFilters", "filtersColors")

        lifecycleScope.launch {
            val pendingRequests = mutableListOf<Job>()

            for (type in inCache.indices) {
                val inCacheType = Globals.getInstance().constCache(inCache[type])

                if (inCacheType == null || Globals.getInstance().isCacheExpired(inCache[type])) {
                    Log.wtf("No valid", "No valid ${inCache[type]} found. Fetching...")
                    val job = launch {
                        DatabaseMethods.Tech().requestGET("constants", "type=$type") { response ->
                            if (response == null) {
                                return@requestGET
                            }
                            val responseBody = response.body?.string()
                            Log.e("Response", responseBody.toString())
                            applyData(type, responseBody ?: "")
                        }
                    }
                    pendingRequests.add(job)
                } else {
                    Log.d("getting from cache", inCacheType)
                    applyData(type, inCacheType)
                }
            }
            pendingRequests.joinAll()
        }
    }
}