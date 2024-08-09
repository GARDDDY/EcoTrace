package com.gy.ecotrace.ui.more.groups

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.events.CreateEventActivity.Companion.viewPager
import com.gy.ecotrace.ui.more.events.CreateEventActivity.CreationAdapter
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep1
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep2
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep3
import com.gy.ecotrace.ui.more.groups.additional.CreateGroupViewModelFactory
import com.gy.ecotrace.ui.more.groups.additional.GroupRepository
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

        val viewPager: ViewPager2 = findViewById(R.id.viewPagerCreateGroup)

        viewPager.adapter =
            CreationAdapter(this, 2)
        viewPager.isUserInputEnabled = false

        val tabTitles = arrayOf("Основное", "Подробно")
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

        sharedViewModel.loadSavedData(GroupRepository.DataStorage().groupData)

        sharedViewModel.groupData.observe(this) {
            if (it.groupName.length >= 5) {
                createGroup.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.ok_green))
                createGroup.setOnClickListener {
                    sharedViewModel.createGroup(currentUser)
                }
            } else {
                createGroup.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.silver))
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