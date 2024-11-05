package com.gy.ecotrace.ui.more.groups

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.CreateGroupViewModelFactory
import com.gy.ecotrace.ui.more.groups.createsteps.CreateGroupStep1
import com.gy.ecotrace.ui.more.groups.createsteps.CreateGroupStep2
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupViewModel

class CreateGroupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_group)
        val currentUser = Globals.getInstance().getString("CurrentlyLogged")
        val currentGroup = Globals.getInstance().getString("CurrentlyWatchingGroup")

        val groupData = Gson().fromJson(intent.getStringExtra("data"), DatabaseMethods.DataClasses.Group::class.java)
        val groupRules = Gson().fromJson(intent.getStringExtra("rules"), Array<String?>::class.java)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }


        val viewPager: ViewPager2 = findViewById(R.id.viewPagerCreateGroup)

        viewPager.adapter =
            CreationAdapter(this, 2)
        viewPager.isUserInputEnabled = false

        val tabTitles = arrayOf(getString(R.string.mainInfo), getString(R.string.extraInfo))
        val tabView: TabLayout = findViewById(R.id.tabLayout)
        TabLayoutMediator(tabView, viewPager) { tab, pos ->
            tab.text = tabTitles[pos]
        }.attach()

        val createGroup: Button = findViewById(R.id.finishCreateGroup)

        val repository = Repository(
            DatabaseMethods.UserDatabaseMethods(),
            DatabaseMethods.ApplicationDatabaseMethods()
        )
        val factory = CreateGroupViewModelFactory(repository)
        val sharedViewModel = ViewModelProvider(this, factory)[CreateGroupViewModel::class.java]

        try {
            sharedViewModel.applyGroupData(groupData, groupRules)
        } catch (e: Exception) {
            sharedViewModel.applyGroupData(DatabaseMethods.DataClasses.GroupChange(), arrayOf(null, null))
        }

        sharedViewModel.groupData.observe(this) {
            if ((it.groupName?.length ?: 0) >= 5) {
                createGroup.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.ok_green)
                createGroup.setOnClickListener {
                    sharedViewModel.createGroup {
                        if (it == null) {
                            Toast.makeText(this@CreateGroupActivity, if (currentGroup == "") "Группа создана!" else "Изменения применены!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@CreateGroupActivity, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                createGroup.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.silver)
                createGroup.setOnClickListener { false }
            }
        }

    }

    class CreationAdapter(fragmentActivity: FragmentActivity, private val totalTabs: Int) : FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CreateGroupStep1()
                else -> CreateGroupStep2()
            }
        }

        override fun getItemCount(): Int {
            return totalTabs
        }
        override fun getItemViewType(position: Int): Int {
            return position
        }
    }
}