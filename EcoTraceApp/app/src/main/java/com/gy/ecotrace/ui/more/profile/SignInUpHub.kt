package com.gy.ecotrace.ui.more.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.gy.ecotrace.R

class SignInUpHub : AppCompatActivity() {
    private lateinit var fragmentContainer: FrameLayout
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
//            .addToBackStack(null)
            .commit()
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainer.id, fragment)
//            .addToBackStack(null)
            .commit()
    }
}