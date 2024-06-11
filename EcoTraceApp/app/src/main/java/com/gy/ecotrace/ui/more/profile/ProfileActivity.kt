package com.gy.ecotrace.ui.more.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R


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
            supportFragmentManager.beginTransaction().replace(R.id.main_profile_activity_layout, ProfileFragment()).commit()
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        val previousId = intent.getStringExtra("previousId")
        if (previousId != null) {
            Globals.getInstance().setString("CurrentlyWatching", previousId)
        }
    }
}