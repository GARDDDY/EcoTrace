package com.gy.ecotrace.ui.more.events

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.network.dto.UserEventItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShowEventsJoinedActivity : AppCompatActivity() {

    private val viewModel: ShowAllEventsViewModel by viewModels()

    @Inject
    lateinit var tokenStore: TokenStore

    private var selectedTiming = 1

    private lateinit var chipUpcoming: Chip
    private lateinit var chipActive: Chip
    private lateinit var chipFinished: Chip
    private lateinit var notFoundEvents: TextView

    private val pendingLeaveIds = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_events_joined)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar10)
        Globals().initToolbarIconBack(toolbar, applicationContext)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        chipUpcoming = findViewById(R.id.upcomingEvents)
        chipActive   = findViewById(R.id.activeEvents)
        chipFinished = findViewById(R.id.finishedEvents)
        notFoundEvents = findViewById(R.id.textNotFoundEvents)
        val container = findViewById<LinearLayout>(R.id.relevantEvents)

        selectChip(chipUpcoming)

        chipUpcoming.setOnClickListener { selectChip(chipUpcoming); rebind(container, viewModel.myEvents.value ?: emptyList()) }
        chipActive.setOnClickListener   { selectChip(chipActive);   rebind(container, viewModel.myEvents.value ?: emptyList()) }
        chipFinished.setOnClickListener { selectChip(chipFinished); rebind(container, viewModel.myEvents.value ?: emptyList()) }

        viewModel.myEvents.observe(this) { events -> rebind(container, events) }
        viewModel.loadMyEvents(tokenStore.userId)
    }

    private fun selectChip(chip: Chip) {
        listOf(chipUpcoming, chipActive, chipFinished).forEach { it.backgroundTintList = getColorStateList(R.color.dirt_white)
        }
        chip.backgroundTintList = getColorStateList(R.color.dirt2_white)
        selectedTiming = when (chip.id) {
            R.id.upcomingEvents -> 0
            R.id.activeEvents   -> 1
            else                -> 2
        }
    }

    private fun roleLabel(role: String): String = when (role) {
        "participant" -> "Участник"
        "assistant"   -> "Помощник"
        "creator"     -> "Организатор"
        else          -> role
    }

    private fun timingLabel(timing: Int): String = when (timing) {
        0    -> "Предстоит"
        1    -> "Идёт"
        else -> "Прошло"
    }

    private fun rebind(container: LinearLayout, events: List<UserEventItem>) {
        container.removeAllViews()
        val filtered = events.filter { it.timing == selectedTiming }
        notFoundEvents.isVisible = filtered.isEmpty()
        for (event in filtered) {
            val row = layoutInflater.inflate(R.layout.layout_event__my_events_with_fast_action_button, container, false)
            row.findViewById<TextView>(R.id.eventName).text    = event.name
            row.findViewById<TextView>(R.id.eventStatus).text  = timingLabel(event.timing)
            row.findViewById<TextView>(R.id.eventMembers).text = event.membersCount.toString()
            row.findViewById<TextView>(R.id.eventRole).text    = roleLabel(event.role)

            row.setOnClickListener {
                Globals.getInstance().setString("CurrentlyWatchingEvent", event.id.toString())
                startActivity(Intent(this, ShowEventActivity::class.java))
            }

            val leaveBtn = row.findViewById<MaterialButton>(R.id.leaveEvent)
            if (event.timing == 0 && event.role != "creator") {
                leaveBtn.visibility = View.VISIBLE
                applyLeaveButtonState(leaveBtn, event.id)
                leaveBtn.setOnClickListener {
                    if (!pendingLeaveIds.add(event.id)) pendingLeaveIds.remove(event.id)
                    applyLeaveButtonState(leaveBtn, event.id)
                }
            } else {
                leaveBtn.visibility = View.GONE
            }

            row.findViewById<View>(R.id.eventTagsScrollView).visibility = View.GONE
            container.addView(row)
        }
    }

    private fun applyLeaveButtonState(btn: MaterialButton, eventId: Long) {
        if (pendingLeaveIds.contains(eventId)) {
            btn.text = "Присоединиться"
            btn.backgroundTintList = getColorStateList(R.color.ok_green)
            btn.setTextColor(getColor(R.color.dirt_white))
        } else {
            btn.text = "Покинуть"
            btn.backgroundTintList = getColorStateList(R.color.dirt_white)
            btn.setTextColor(getColor(R.color.ok_green))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        for (eventId in pendingLeaveIds) viewModel.leaveEvent(eventId) {}
    }
}
