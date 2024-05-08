package com.gy.ecotrace

import android.content.Context
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.gy.ecotrace.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        setSupportActionBar()
        val toolbar : Toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_news, R.id.navigation_education, R.id.navigation_ratings, R.id.navigation_more
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val localSettings = getSharedPreferences("localValues", Context.MODE_PRIVATE)
        if (!localSettings.contains("created")){
            val creator = localSettings.edit()
            creator.putBoolean("created", true)
            creator.putString("loggedId", "0")
            creator.apply()
        }
        Globals.getInstance().setString("CurrentlyLogged", localSettings.getString("loggedId", "0"))
        Globals.getInstance().setString("CurrentlyWatching", localSettings.getString("loggedId", "0"))
        FirebaseMethods().appCont(this)
    }
}