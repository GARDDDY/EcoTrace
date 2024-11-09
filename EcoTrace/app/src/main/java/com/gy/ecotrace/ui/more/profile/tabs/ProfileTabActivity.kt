package com.gy.ecotrace.ui.more.profile.tabs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.network.dto.UserEventItem
import com.gy.ecotrace.ui.more.events.ShowEventActivity
import com.gy.ecotrace.ui.more.profile.ProfileViewModel

fun showEventsSortMenu(context: Context, anchor: View, viewModel: ProfileViewModel) {
    val popup = PopupMenu(context, anchor)
    popup.menuInflater.inflate(R.menu.popup_menu_profile_events_sort, popup.menu)
    popup.setOnMenuItemClickListener { item ->
        val position = if (item.itemId == R.id.sortByMembers) 1 else 0
        if (viewModel.lastEventSort != position) {
            viewModel.lastEventSort = position
            viewModel.loadEvents(viewModel.currentUserId, if (position == 1) "members" else null, true)
        }
        true
    }
    popup.show()
}

class ProfileTabActivity : Fragment() {

    private val viewModel: ProfileViewModel by activityViewModels()

    private val pendingLeaveIds = mutableSetOf<Long>()

    private fun eventRoleLabel(role: String?): String = when (role) {
        "creator" -> "Создатель"
        "assistant" -> "Помощник"
        else -> "Участник"
    }

    private fun eventStatusLabel(status: String?): String = when (status) {
        "active", "ongoing" -> "Идёт"
        "upcoming", "planned", "pending" -> "Предстоит"
        "finished", "ended", "past" -> "Прошло"
        else -> status ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_layout_profile__activity, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventsLayout = view.findViewById<LinearLayout>(R.id.eventsLayout)

        if (viewModel.events.value.isNullOrEmpty() && !viewModel.foundAllEvents) {
            viewModel.loadEvents(viewModel.currentUserId, sortParam(viewModel.lastEventSort))
        }
        viewModel.events.observe(viewLifecycleOwner, Observer { events ->
            view.findViewById<ShimmerFrameLayout>(R.id.userEventsInformationLayoutLoading).visibility = View.GONE

            if (events.isEmpty() && eventsLayout.childCount == 0) {
                view.findViewById<TextView>(R.id.noEventsWarning).visibility = View.VISIBLE
                eventsLayout.visibility = View.GONE
                return@Observer
            }
            view.findViewById<TextView>(R.id.noEventsWarning).visibility = View.GONE
            eventsLayout.visibility = View.VISIBLE

            if (viewModel.newEvents) {
                eventsLayout.removeAllViews()
                pendingLeaveIds.clear()
                viewModel.newEvents = false
            }
            for (event in events) addEventRow(eventsLayout, event)
        })
    }

    private fun sortParam(position: Int): String? = if (position == 1) "members" else null

    private fun addEventRow(container: LinearLayout, event: UserEventItem) {
        val row = layoutInflater.inflate(R.layout.layout_event__my_events_with_fast_action_button, container, false)
        row.findViewById<TextView>(R.id.eventName).text = event.name
        val members = row.findViewById<TextView>(R.id.eventMembers)
        val userRole = row.findViewById<TextView>(R.id.eventRole)
        userRole.text = eventRoleLabel(event.role)
        if (!event.validated) {
            userRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_no))
        }
        row.findViewById<TextView>(R.id.eventStatus).text = eventStatusLabel(event.status)

        val ending = when {
            event.membersCount % 100 in 11..14 -> "ов"
            event.membersCount % 10 == 1 -> ""
            event.membersCount % 10 in 2..4 -> "а"
            else -> "ов"
        }
        members.text = "${event.membersCount} участник$ending"

        row.setOnClickListener {
            Globals.getInstance().setString("CurrentlyWatchingEvent", event.id.toString())
            startActivity(Intent(requireActivity(), ShowEventActivity::class.java))
        }

        val leaveBtn = row.findViewById<MaterialButton>(R.id.leaveEvent)
        if (viewModel.isSelf && event.timing == 0 && event.role != "creator") {
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

    private fun applyLeaveButtonState(btn: MaterialButton, eventId: Long) {
        if (pendingLeaveIds.contains(eventId)) {
            btn.text = "Присоединиться"
            btn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.ok_green)
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.dirt_white))
        } else {
            btn.text = "Покинуть"
            btn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.dirt_white)
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.ok_green))
        }
    }

    override fun onDestroyView() {
        for (eventId in pendingLeaveIds) viewModel.leaveEvent(eventId) {}
        pendingLeaveIds.clear()
        super.onDestroyView()
    }
}
