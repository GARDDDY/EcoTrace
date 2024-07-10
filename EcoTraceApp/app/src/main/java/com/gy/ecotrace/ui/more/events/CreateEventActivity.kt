package com.gy.ecotrace.ui.more.events

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep1
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep2
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep3
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Text

class CreateEventActivity : AppCompatActivity() {
    companion object {
        lateinit var viewPager: ViewPager2
        lateinit var mainlayout: ConstraintLayout

        var eventClass = DatabaseMethods.DataClasses.Event()
        var eventmoreClass = DatabaseMethods.ApplicationDatabaseMethods.EventMore()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MapKitFactory.initialize(applicationContext)
        setContentView(R.layout.activity_create_event)

        val editingEvent = Globals.getInstance().getString("CurrentlyEditingEvent")
        if (editingEvent != "0") {

            lifecycleScope.launch {
                eventClass = withContext(Dispatchers.IO) { DatabaseMethods.ApplicationDatabaseMethods().getEvent(editingEvent)}
                eventmoreClass = withContext(Dispatchers.IO) { DatabaseMethods.ApplicationDatabaseMethods().getEventMore(editingEvent)}

                defineGlobalWidgets()
                listeners()
                finishLoading()
            }
        } else {
            finishLoading()
            defineGlobalWidgets()
            listeners()
        }


    }

    private fun finishLoading() {
        findViewById<ViewPager2>(R.id.viewPagerCreateEvent).visibility = View.VISIBLE
        findViewById<ProgressBar>(R.id.progressBarLoadEdit).visibility = View.GONE
        findViewById<TextView>(R.id.textView31).foreground =
            ColorDrawable(ContextCompat.getColor(this, R.color.transparent))
        findViewById<TextView>(R.id.textView33).foreground =
            ColorDrawable(ContextCompat.getColor(this, R.color.transparent))
    }

    private fun defineGlobalWidgets(){
        mainlayout = findViewById(R.id.main)
        viewPager = findViewById(R.id.viewPagerCreateEvent)

        viewPager.adapter = CreationAdapter(this, 3)
        viewPager.isUserInputEnabled = false
    }

    private fun listeners(){
        val nextBtn: Button = findViewById(R.id.nextPageBtn)
        val prevBtn: Button = findViewById(R.id.prevPageBtn)
        val endCreation: Button = findViewById(R.id.endCreation)

        nextBtn.setOnClickListener {
            if (eventClass.eventName.length > 5) {
                val nextVal = (viewPager.currentItem + 1) % 3//CreationAdapter().itemCount
                viewPager.currentItem = nextVal
                updateOnItem(nextVal)
            } else {
                Toast.makeText(this, "Пожалуйста, введите название своего мероприятия!", Toast.LENGTH_SHORT).show()
            }
        }
        prevBtn.setOnClickListener {
            if (viewPager.isUserInputEnabled) {
                val nextVal = (viewPager.currentItem - 1)%3//CreationAdapter().itemCount
                viewPager.currentItem = nextVal
                updateOnItem(nextVal)
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateOnItem(position)
            }
        })

        endCreation.setOnClickListener {
            CreateEventStep2().goalsReturn().forEach {
                eventmoreClass.eventGoals["g${eventmoreClass.eventGoals.size}"] = it
            }
            eventmoreClass.eventTimes = CreateEventStep2().timesReturn()
            CreateEventStep3().mapObj().forEach { (_, dClass) ->
                eventmoreClass.eventCoords["c${eventmoreClass.eventCoords.size}"] = dClass
            }
            createEvent()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun createEvent(){
        GlobalScope.launch {
            val eventId = DatabaseMethods.ApplicationDatabaseMethods().createEvent(eventClass, eventmoreClass)

            Globals.getInstance().setString("CurrentlyWatchingEvent", eventId)
            startActivity(Intent(this@CreateEventActivity, ShowEventActivity::class.java))
            finish()
        }
    }

    fun updateOnItem(position: Int) {
        val nextBtn: Button = findViewById(R.id.nextPageBtn)
        val prevBtn: Button = findViewById(R.id.prevPageBtn)
        val endCreation: Button = findViewById(R.id.endCreation)
            when (position) {
                0 -> {
                    prevBtn.visibility = View.GONE
                    endCreation.visibility = View.GONE
                    findViewById<TextView>(R.id.textView33).text = "Краткая информация"
                    val constraintLayout = findViewById<ConstraintLayout>(R.id.main)
                    val constraintSet = ConstraintSet()
                    constraintSet.clone(constraintLayout)
                    constraintSet.setHorizontalBias(R.id.nextPageBtn, 0.5f)
                    constraintSet.applyTo(constraintLayout)
                }
                1 -> {
                    nextBtn.visibility = View.VISIBLE
                    prevBtn.visibility = View.VISIBLE
                    endCreation.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.textView33).text = "Опишите свою идею подробнее"
                    val constraintLayout = findViewById<ConstraintLayout>(R.id.main)
                    val constraintSet = ConstraintSet()
                    constraintSet.clone(constraintLayout)
                    constraintSet.setHorizontalBias(R.id.nextPageBtn, 0.75f)
                    constraintSet.setHorizontalBias(R.id.prevPageBtn, 0.25f)
                    constraintSet.applyTo(constraintLayout)
                }
                2 -> {
                    nextBtn.visibility = View.GONE
                    endCreation.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.textView33).text = "Добавьте события на карту"
                    val constraintLayout = findViewById<ConstraintLayout>(R.id.main)
                    val constraintSet = ConstraintSet()
                    constraintSet.clone(constraintLayout)
                    constraintSet.setHorizontalBias(R.id.prevPageBtn, 0.5f)
                    constraintSet.applyTo(constraintLayout)
                }
            }
            findViewById<TextView>(R.id.textView31).text = "Шаг ${position+1}/${3}"
        }

    class CreationAdapter(fragmentActivity: FragmentActivity, private val totalTabs: Int) : FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CreateEventStep1()
                1 -> CreateEventStep2()
                else -> CreateEventStep3()
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