package com.gy.ecotrace.ui.more.events.showtabs

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventViewModel
import com.yandex.mapkit.search.Line

class ShowEventStep1 : Fragment() {

    private val sharedViewModel: ShowEventViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activitylayout_show_event_step1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentEvent = sharedViewModel.currentEvent

        Glide.with(requireActivity())
            .load(Globals().getImgUrl("events", currentEvent))
            .placeholder(R.drawable.round_family_restroom_24)
            .into(view.findViewById(R.id.eventImage))

//        sharedViewModel.getEvent()
        sharedViewModel.event.observe(viewLifecycleOwner, Observer { eventUser ->
            val event = eventUser.eventInfo
            view.findViewById<TextView>(R.id.eventName).text = event.eventName
            view.findViewById<TextView>(R.id.eventAbout).text = event.eventAbout
            view.findViewById<TextView>(R.id.eventCountMembers).text = event.eventCountMembers.toString()
            view.findViewById<TextView>(R.id.eventStartDate).text = when(event.eventStatus) {
                0 -> "еще не началось"
                1 -> "уже проходит"
                2 -> "закончилось"
                else -> "неизвестно"
            }

            val tags = DatabaseMethods.DataClasses.EventFiltersSearchBy
            val colors = DatabaseMethods.DataClasses.filterColors
            for (tag in event.filters.split(',').map { it.toInt()-1 }) {
                val filter = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
                filter.text = tags[tag].first
                filter.textSize = 18F
                filter.setTextColor(Color.parseColor(colors[tag].second))
                filter.setBackgroundColor(Color.parseColor(colors[tag].first))

                view.findViewById<LinearLayout>(R.id.eventTagsLayout).addView(filter)
            }

            val joinBtn = view.findViewById<Button>(R.id.joinEvent)

            if (eventUser.isUserInEvent) {
                joinBtn.text = "Покинуть"
                joinBtn.setOnClickListener {
                    if (!eventUser.isUserCreator) {
                        sharedViewModel.leaveEvent()
                    } else {
                        // unable to leave alert!!!
                    }
                }
            } else {
                joinBtn.setOnClickListener {
                    sharedViewModel.joinEvent()
                }
            }
        })

        val allGoals: LinearLayout = view.findViewById(R.id.allGoalsLayout)

        sharedViewModel.getGoals()
        sharedViewModel.eventGoals.observe(viewLifecycleOwner, Observer {
            it?.let {
                for (goal in it.indices) {
                    val goalLayout = layoutInflater.inflate(R.layout.layout_event_goaltime, null)
                    goalLayout.findViewById<TextView>(R.id.objectName).text = "${goal+1}. ${Html.fromHtml(it[goal], Html.FROM_HTML_MODE_LEGACY)}"
                    allGoals.addView(goalLayout)
                }
            }
        })

    }
}