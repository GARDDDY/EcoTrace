package com.gy.ecotrace.ui.more.events

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep1
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep2
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep3
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep4
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventViewModel
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventViewModelFactory
import com.yandex.mapkit.MapKitFactory

class CreateEventActivity : AppCompatActivity() {
    private lateinit var sharedViewModel: CreateEventViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MapKitFactory.initialize(applicationContext)
        setContentView(R.layout.activity_create_event)

        val toolbar = findViewById<Toolbar>(R.id.toolbar9)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        val factory = CreateEventViewModelFactory(repository)
        sharedViewModel = ViewModelProvider(this, factory)[CreateEventViewModel::class.java]

        val editingEvent = Globals.getInstance().getString("CurrentlyEditingEvent")
        sharedViewModel.getEvent(editingEvent)

        val pagesNames = listOf("Основное", "Планирование", "Цели", "Карта")
        val nextBtn: ImageButton = findViewById(R.id.nextPageBtn)
        val prevBtn: ImageButton = findViewById(R.id.prevPageBtn)
        val endCreation: Button = findViewById(R.id.endCreation)

        val viewPager: ViewPager2 = findViewById(R.id.viewPagerCreateEvent)

        viewPager.adapter = CreationAdapter(this, pagesNames.size)
        viewPager.isUserInputEnabled = false

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                findViewById<TextView>(R.id.textView31).text = pagesNames[position]
                findViewById<TextView>(R.id.textView33).text = "Шаг ${position+1} / ${pagesNames.size}"

                endCreation.visibility = if (position == 0) View.GONE else View.VISIBLE

                val lParams = viewPager.layoutParams as LayoutParams
                lParams.width = if (position == 3) LayoutParams.MATCH_PARENT else 0
                viewPager.layoutParams = lParams
            }
        })



        nextBtn.setOnClickListener {
            if ((sharedViewModel.eventData.value?.eventName?.length ?: 0) > 5) {
                val nextVal = (viewPager.currentItem + 1) % pagesNames.size

                if (nextVal == pagesNames.size-1) {
                    nextBtn.visibility = View.INVISIBLE
                }

                viewPager.currentItem = nextVal
                prevBtn.visibility = View.VISIBLE

            } else {
                Toast.makeText(this, "Пожалуйста, введите название своего мероприятия!", Toast.LENGTH_SHORT).show()
            }
        }
        prevBtn.setOnClickListener {
            val nextVal = (viewPager.currentItem - 1) % pagesNames.size

            if (nextVal == 0) {
                prevBtn.visibility = View.INVISIBLE
            }

            viewPager.currentItem = nextVal
            nextBtn.visibility = View.VISIBLE

        }


    }

    class CreationAdapter(fragmentActivity: FragmentActivity, private val totalTabs: Int) : FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CreateEventStep1()
                1 -> CreateEventStep2()
                2 -> CreateEventStep3()
                else -> CreateEventStep4()
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