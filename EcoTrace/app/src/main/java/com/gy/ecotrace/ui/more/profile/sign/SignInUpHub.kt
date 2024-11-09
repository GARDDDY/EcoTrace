package com.gy.ecotrace.ui.more.profile.sign

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.gy.ecotrace.Globals
import com.gy.ecotrace.ui.more.profile.ProfileActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInUpHub : AppCompatActivity() {
    private lateinit var fragmentContainer: FrameLayout
    private val signHubViewModel: SignHubViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentContainer = FrameLayout(this)
        fragmentContainer.id = View.generateViewId()
        setContentView(fragmentContainer)

        if (savedInstanceState == null) {
            addFragment(SignInFragment())
        }
    }

    fun addFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainer.id, fragment)
            .commit()
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainer.id, fragment)
            .commit()
    }

    private fun unavailable() {
        Toast.makeText(this, "Не удалось войти в аккаунт!", Toast.LENGTH_SHORT).show()
    }

    fun signIn(login: String, password: String) {
        signHubViewModel.signIn(login, password) { user ->
            if (user == null) {
                unavailable()
                return@signIn
            }
            Globals.getInstance().setString("CurrentlyWatching", user.id.toString())
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}
