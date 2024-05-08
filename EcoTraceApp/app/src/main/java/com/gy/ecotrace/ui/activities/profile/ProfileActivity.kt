package com.gy.ecotrace.ui.activities.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import java.security.MessageDigest


class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        val currentUser = Globals.getInstance().getString("CurrentlyWatching")

        if (currentUser == "0") {
            Log.e("Profile", "Not logged")
            val intent = Intent(this, SignInUpHub::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }
        else {
            Log.e("Profile", "Logged")
            val loggedUser = ProfileFragment()
            supportFragmentManager.beginTransaction().replace(R.id.main_profile_activity_layout, loggedUser).commit()
        }

    }
}