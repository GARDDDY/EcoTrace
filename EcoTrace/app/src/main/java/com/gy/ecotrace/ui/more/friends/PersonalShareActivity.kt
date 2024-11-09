package com.gy.ecotrace.ui.more.friends

import android.animation.ObjectAnimator
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.customs.QrCache
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.repos.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PersonalShareActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var tokenStore: TokenStore

    private val mainHost = BuildConfig.SERVER_API_URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personal_share)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val personalQr: ImageView = findViewById(R.id.userQrCode)
        val shareName: TextView = findViewById(R.id.shareName)
        val content = intent.getStringExtra("link") ?: "${mainHost}user/${tokenStore.userId}"
        val fg = ContextCompat.getColor(applicationContext, R.color.ok_green)
        val bg = ContextCompat.getColor(applicationContext, R.color.dirt2_white)

        lifecycleScope.launch {
            val file = QrCache.ensure(applicationContext, content, fg, bg)
            val bmp = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path) }
            if (bmp != null) {
                personalQr.setImageBitmap(bmp)
                ObjectAnimator.ofFloat(personalQr, "alpha", 0f, 1f).apply {
                    duration = 600
                    interpolator = AccelerateInterpolator()
                }.start()
            }
        }

        val imageKey = intent.getStringExtra("image")
        val bucket = intent.getStringExtra("imageF") ?: "users"
        lifecycleScope.launch {
            val cached = userRepository.cachedCurrentUser()
            shareName.text = cached?.username ?: ""
            val url = when {
                imageKey != null -> "${mainHost}uploads/$bucket/$imageKey"
                cached?.avatarKey != null -> "${mainHost}uploads/users/${cached.avatarKey}"
                else -> null
            }
            Glide.with(this@PersonalShareActivity)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .into(findViewById(R.id.userImage))
        }
    }

    override fun onResume() {
        super.onResume()
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window.attributes = params
    }

    override fun onPause() {
        super.onPause()
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params
    }
}
