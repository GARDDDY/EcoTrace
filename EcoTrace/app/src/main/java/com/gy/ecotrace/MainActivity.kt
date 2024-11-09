package com.gy.ecotrace

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.gy.ecotrace.app.ServerStateViewModel
import com.gy.ecotrace.customs.QrCache
import com.gy.ecotrace.databinding.ActivityMainBinding
import com.gy.ecotrace.network.TokenStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (tokenStore.userId > 0) {
            lifecycleScope.launch {
                QrCache.ensure(
                    applicationContext,
                    "${BuildConfig.SERVER_API_URI}user/${tokenStore.userId}",
                    ContextCompat.getColor(this@MainActivity, R.color.ok_green),
                    ContextCompat.getColor(this@MainActivity, R.color.dirt2_white)
                )
            }
        }

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        binding.navView.setupWithNavController(navHost.navController)

        val serverViewModel: ServerStateViewModel by viewModels()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                serverViewModel.hasConnection.collect { online ->
                    binding.connectionBanner.visibility = if (online) View.GONE else View.VISIBLE
                }
            }
        }

        lifecycleScope.launch {
            delay(2000)
            binding.splashOverlay.animate()
                .alpha(0f)
                .setDuration(400)
                .withEndAction { binding.splashOverlay.visibility = View.GONE }
                .start()
        }
    }
}
