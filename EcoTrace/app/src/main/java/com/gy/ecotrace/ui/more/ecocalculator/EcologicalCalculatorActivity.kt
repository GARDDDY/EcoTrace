package com.gy.ecotrace.ui.more.ecocalculator

import android.os.Bundle
import android.widget.Button
import android.widget.Toolbar
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
import com.gy.ecotrace.ui.more.ecocalculator.views.EcoCalculatorMainFragment
import com.gy.ecotrace.ui.more.ecocalculator.views.EcoCalculatorResultsFragment
import com.gy.ecotrace.ui.more.groups.additional.CreateGroupViewModelFactory
import com.gy.ecotrace.ui.more.groups.createsteps.CreateGroupStep1
import com.gy.ecotrace.ui.more.groups.createsteps.CreateGroupStep2
import com.gy.ecotrace.ui.more.groups.viewModels.CreateGroupViewModel

class EcologicalCalculatorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ecological_calculator)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val viewPager: ViewPager2 = findViewById(R.id.viewPagerEcoCalc)

        viewPager.adapter =
            CreationAdapter(this, 2)
        viewPager.isUserInputEnabled = false

        val tabTitles = arrayOf(getString(R.string.calculators), getString(R.string.results))
        val tabView: TabLayout = findViewById(R.id.tabLayout)
        TabLayoutMediator(tabView, viewPager) { tab, pos ->
            tab.text = tabTitles[pos]
        }.attach()

        val repository = Repository(
            DatabaseMethods.UserDatabaseMethods(),
            DatabaseMethods.ApplicationDatabaseMethods()
        )
        val factory = EcoCalcViewModelFactory(repository)
        val sharedViewModel = ViewModelProvider(this, factory)[EcoCalcViewModel::class.java]

    }

    class CreationAdapter(fragmentActivity: FragmentActivity, private val totalTabs: Int) : FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> EcoCalculatorMainFragment()
                else -> EcoCalculatorResultsFragment()
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