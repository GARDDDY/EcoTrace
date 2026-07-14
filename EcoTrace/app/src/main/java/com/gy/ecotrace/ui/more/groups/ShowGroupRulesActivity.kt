package com.gy.ecotrace.ui.more.groups

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.repos.GroupRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShowGroupRulesActivity : AppCompatActivity() {

    @Inject
    lateinit var groupRepository: GroupRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_group_rules)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val groupId = Globals.getInstance().getString("CurrentlyWatchingGroup").toLongOrNull() ?: 0
        lifecycleScope.launch {
            val group = runCatching {
                groupRepository.cachedGroup(groupId) ?: groupRepository.refreshGroup(groupId)
            }.getOrNull()

            findViewById<TextView>(R.id.groupRulesText).text =
                group?.rules ?: getString(R.string.noRulesInThisGroupYet)

            val rulesImage = findViewById<ImageView>(R.id.groupRulesImage)
            if (group?.imageGenerated == true) {
                rulesImage.visibility = View.GONE
            } else {
                val imageUrl = group?.rulesImageKey?.let { "${BuildConfig.SERVER_API_URI}uploads/groups/$it" }
                Glide.with(this@ShowGroupRulesActivity).load(imageUrl).into(rulesImage)
            }
        }
    }
}
