package com.gy.ecotrace.ui.more.groups

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.yandex.mapkit.search.Line
import java.sql.Time
import java.util.Locale

class ShowGroupActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private fun animationColorChange(colorFrom: Int, colorTo: Int) {
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 300
        colorAnimation.addUpdateListener { animator ->
            toolbar.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimation.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_group)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        toolbar = findViewById(R.id.toolbar4)
        val backIcon = ContextCompat.getDrawable(applicationContext, R.drawable.baseline_arrow_back_24)
        backIcon?.setColorFilter(ContextCompat.getColor(applicationContext, R.color.black), PorterDuff.Mode.SRC_ATOP)
        toolbar.navigationIcon = backIcon
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val currentGroup = Globals.getInstance().getString("CurrentlyWatchingGroup")
        val currentUser = Globals.getInstance().getString("CurrentlyLogged")

        Glide.with(this@ShowGroupActivity)
            .load(Globals().getImgUrl("groups", currentGroup))
            .placeholder(R.drawable.round_family_restroom_24)
            .into(findViewById(R.id.groupImage))

        val allPostsLayout: LinearLayout = findViewById(R.id.groupNewsLayout) //// test
        for (i in 1..10) {
            val postLayout = layoutInflater.inflate(R.layout.layout_user_post_in_group, null)
            if (currentUser == "0") {
                postLayout.findViewById<LinearLayout>(R.id.bottomInfoPost).visibility = View.GONE
            } else {
                Glide.with(this)
                    .load(Globals().getImgUrl("users", currentUser))
                    .circleCrop()
                    .into(postLayout.findViewById(R.id.currentUserImage))
            }


            allPostsLayout.addView(postLayout)
        }

        val separator: View = findViewById(R.id.separatorColor)
        val scrollView: ScrollView = findViewById(R.id.mainScrollViewGroupNews)
        var fHideTSee = false
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val scrollBounds = Rect()
            scrollView.getHitRect(scrollBounds)
            if (!separator.getLocalVisibleRect(scrollBounds)) {
                if (!fHideTSee) {
                    animationColorChange(
                        ContextCompat.getColor(this, R.color.transparent),
                        ContextCompat.getColor(this, R.color.dirt_white)
                    )
                    toolbar.title = currentGroup
                    fHideTSee = true
                }
            } else {
                if (fHideTSee) {
                    animationColorChange(
                        ContextCompat.getColor(this, R.color.dirt_white),
                        ContextCompat.getColor(this, R.color.transparent)
                    )
                    toolbar.title = ""
                    fHideTSee = false
                }
            }
        }

    }
}