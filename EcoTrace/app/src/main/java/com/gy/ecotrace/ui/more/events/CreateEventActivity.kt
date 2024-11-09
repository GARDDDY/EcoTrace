package com.gy.ecotrace.ui.more.events

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep1
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventStep2
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateEventActivity : AppCompatActivity() {
    private val sharedViewModel: CreateEventViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_event__create)

        val toolbar = findViewById<Toolbar>(R.id.toolbar9)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val editEventId = intent.getLongExtra("EditEventId", 0).takeIf { it != 0L }

        if (editEventId != null) {
            sharedViewModel.loadForEdit(editEventId) { ok ->
                if (!ok) {
                    Toast.makeText(this, "Не удалось загрузить мероприятие для редактирования", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else if (sharedViewModel.hasDraft()) {
            AlertDialog.Builder(this)
                .setTitle("Продолжить создание?")
                .setMessage("Есть незавершённое мероприятие. Продолжить заполнение?")
                .setPositiveButton("Продолжить") { _, _ -> sharedViewModel.restoreDraft() }
                .setNegativeButton("Начать заново") { _, _ ->
                    sharedViewModel.clearDraft()
                    sharedViewModel.getEvent()
                }
                .setCancelable(false)
                .show()
        } else {
            sharedViewModel.getEvent()
        }

        val pagesNames = listOf(getString(R.string.mainInfo), getString(R.string.map))
        val nextBtn: ImageButton = findViewById(R.id.nextPageBtn)
        val prevBtn: ImageButton = findViewById(R.id.prevPageBtn)
        val endCreation: Button = findViewById(R.id.endCreation)
        if (editEventId != null) endCreation.text = "Сохранить"

        val viewPager: ViewPager2 = findViewById(R.id.viewPagerCreateEvent)

        viewPager.adapter = CreationAdapter(this, 2)
        viewPager.isUserInputEnabled = false

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                findViewById<TextView>(R.id.textView31).text = pagesNames[position]
                findViewById<TextView>(R.id.textView33).text = "${getString(R.string.step)} ${position+1} / ${pagesNames.size}"

                if ((sharedViewModel.eventData.value?.eventStatus ?: 0) == 0) {
                    endCreation.visibility = if (position == 0) View.GONE else View.VISIBLE
                } else endCreation.visibility = View.GONE

                val lParams = viewPager.layoutParams as LayoutParams
                lParams.width = if (position == 1) LayoutParams.MATCH_PARENT else 0
                viewPager.layoutParams = lParams
            }
        })



        nextBtn.setOnClickListener {
            if ((sharedViewModel.eventData.value?.eventName?.length ?: 0) > 5) {
                val nextVal = (viewPager.currentItem + 1) % pagesNames.size

                if (nextVal == pagesNames.size-1) {
                    nextBtn.visibility = View.GONE
                }

                viewPager.currentItem = nextVal
                prevBtn.visibility = View.VISIBLE

            } else {
                Toast.makeText(this, getString(R.string.enterYouEventName), Toast.LENGTH_SHORT).show()
            }
        }
        prevBtn.setOnClickListener {
            val nextVal = (viewPager.currentItem - 1) % pagesNames.size

            if (nextVal == 0) {
                prevBtn.visibility = View.GONE
            }

            viewPager.currentItem = nextVal
            nextBtn.visibility = View.VISIBLE

        }


        endCreation.setOnClickListener {

            if ((sharedViewModel.eventTimes.value?.size ?: 0) == 0) {
                Toast.makeText(this@CreateEventActivity, getString(R.string.addAtLeastOneTimeAction), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sharedViewModel.submitEvent { eventId, error ->
                if (eventId != null) {
                    if (sharedViewModel.editEventId == null) {
                        Globals.getInstance().setString("CurrentlyWatchingEvent", eventId)
                        startActivity(Intent(this@CreateEventActivity, ShowEventActivity::class.java))
                    }
                    finish()
                } else {
                    Toast.makeText(
                        this@CreateEventActivity,
                        error ?: "Не удалось сохранить мероприятие",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        }

    }

    class CreationAdapter(fragmentActivity: FragmentActivity, private val totalTabs: Int) : FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> CreateEventStep1()
            else -> CreateEventStep2()
        }

        override fun getItemCount(): Int = totalTabs
        override fun getItemViewType(position: Int): Int = position
    }
}