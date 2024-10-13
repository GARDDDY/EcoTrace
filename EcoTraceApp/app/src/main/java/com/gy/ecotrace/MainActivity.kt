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


        val loggedUser = FirebaseAuth.getInstance().currentUser?.uid
        Globals.getInstance().setString("CurrentlyWatching", loggedUser ?: "0")

        startService(Intent(this, AppService::class.java))

        val key = "f3d745ad-1974-4793-978d-52b3a165865c"
        MapKitFactory.setApiKey(key)
        Glide.get(this).clearMemory()

        fun applyData(type: Int, data1: String) {
            Log.d("data", data1)
            val data: Any = try {
                Log.d("setting to array", data1)
                // Попробуем распарсить как массив строк
                Gson().fromJson(data1, Array<String>::class.java)
            } catch (e: Exception) {
                Log.e("ParseError", "Failed to parse as Array<String>: ${e.message}")
                try {
                    // Попробуем распарсить как JsonObject
                    val jsonObject = Gson().fromJson(data1, JsonObject::class.java)
                    Log.d("setting to compl array", "$jsonObject")

                    // Создаем массив пар из JsonObject
                    val pairs = jsonObject.entrySet().map { Pair(it.key, it.value.asString) }.toTypedArray()
                    pairs
                } catch (e: Exception) {
                    Log.e("ParseError", "Failed to parse as JsonObject: ${e.message}")
                    throw Exception("ParsingError $data1\n$e")
                }
            }

            Log.d("data -> $type", data.toString())

            when (type) {
                0 -> Globals.getInstance().setEventRoles(data as Array<String>)
                1 -> Globals.getInstance().setGroupRoles(data as Array<String>)
                2 -> Globals.getInstance().setUserRoles(data as Array<String>)

                3 -> Globals.getInstance().setUserFilters(data as Array<Pair<String, String>>)
                4 -> Globals.getInstance().setEventsFilters(data as Array<Pair<String, String>>)
                5 -> Globals.getInstance().setGroupsFilters(data as Array<Pair<String, String>>)
                6 -> Globals.getInstance().setFiltersColors(data as Array<Pair<String, String>>)
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
                    applyData(type, inCacheType)
                }
            }
            pendingRequests.joinAll()
        }
    }
}