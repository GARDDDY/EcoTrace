package com.gy.ecotrace.ui.more.profile.sign

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gy.ecotrace.Globals
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.profile.ProfileActivity

class SignInUpHub : AppCompatActivity() {
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var signHubViewModel: SignHubViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentContainer = FrameLayout(this)
        fragmentContainer.id = View.generateViewId()
        setContentView(fragmentContainer)

        if (savedInstanceState == null) {
            addFragment(SignInFragment())
        }

        val repository = Repository(
            DatabaseMethods.UserDatabaseMethods(),
            DatabaseMethods.ApplicationDatabaseMethods()
        )
        val factory = SignHubViewModelFactory(repository)
        signHubViewModel = ViewModelProvider(this, factory)[SignHubViewModel::class.java]
    }

    fun addFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainer.id, fragment)
//            .addToBackStack(null)
            .commit()
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainer.id, fragment)
//            .addToBackStack(null)
            .commit()
    }

    private fun unavailable() {
        Toast.makeText(
            this,
            "Не удалось войти в аккаунт!",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun signIn(login: String, password: String) {
        signHubViewModel.getUserEmail(login, password) { email ->
            if (email == null) {
                unavailable()
                return@getUserEmail
            }

            DatabaseMethods.Account().loginIntoAccount(email, password) { uid ->
                if (uid == null) {
                    runOnUiThread {
                        Toast.makeText(this, "Не удалось войти в аккаунт!", Toast.LENGTH_SHORT).show()
                    }
                    return@loginIntoAccount
                }
                Globals.getInstance().setString("CurrentlyWatching", uid ?: "0")

                startActivity(Intent(this, ProfileActivity::class.java))
                finish()
            }
        }
    }

}