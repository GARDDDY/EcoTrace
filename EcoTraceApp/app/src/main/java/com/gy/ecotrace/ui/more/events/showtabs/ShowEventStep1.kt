package com.gy.ecotrace.ui.more.events.showtabs

import android.app.ProgressDialog.show
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.material3.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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

    private fun members(value: Int): String {
        return when {
            value % 100 in 11..14 -> "участников"
            value % 10 == 1 -> "участник"
            value % 10 in 2..4 -> "участника"
            else -> "участников"
        }
    }

    private fun getWhenStart(startString: String): String {
        val values = startString.split(';').map { it.toInt() }
        return when (values[0]) {
            0 -> ", когда начнется первое временное событие"
            1 -> ", когда наберется ${values[1]} ${members(values[1])}"
            else -> "-"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentEvent = sharedViewModel.currentEvent

        val eventImage: ImageView = view.findViewById(R.id.eventImage)
        Glide.with(requireActivity())
            .load(Globals().getImgUrl("events", currentEvent))
            .listener(object : RequestListener<Drawable> { // important!!!
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    eventImage.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    eventImage.visibility = View.VISIBLE
                    return false
                }
            })
            .into(eventImage)

//        sharedViewModel.getEvent()
        sharedViewModel.event.observe(viewLifecycleOwner, Observer { eventUser ->
            val event = eventUser.eventInfo
            view.findViewById<TextView>(R.id.eventName).text = event.eventName
            view.findViewById<TextView>(R.id.eventAbout).text = event.eventAbout
            view.findViewById<TextView>(R.id.eventCountMembers).text = "${event.eventCountMembers} ${members(event.eventCountMembers)}"
            view.findViewById<TextView>(R.id.eventStartDate).text = when(event.eventStatus) {
                0 -> "Начнется ${event.startTime}"
                1 -> "Проходит"
                2 -> "Закончилось"
                else -> "неизвестно"
            }

            val tags: Array<Pair<String, String>> = Globals.getInstance().getEventsFilters()
            val colors: Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
            for (tag in event.filters.split(',').map { it.toInt()-1 }) {
                val filter = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
                filter.text = tags[tag].first
                filter.textSize = 18F
                filter.setTextColor(Color.parseColor(colors[tag].second))
                filter.setBackgroundColor(Color.parseColor(colors[tag].first))

                view.findViewById<LinearLayout>(R.id.eventTagsLayout).addView(filter)
            }

            val joinBtn = view.findViewById<Button>(R.id.joinEvent)

            if (eventUser.eventInfo.eventStatus > 0) {
                joinBtn.visibility = View.GONE
            }

            if (eventUser.isUserInEvent) {
                joinBtn.text = "Покинуть"
                joinBtn.setOnClickListener {
                    if (eventUser.eventRole != 0) {
                        sharedViewModel.leaveEvent()
                    } else {
                        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        builder.setTitle("Вы создатель!")

                        builder.setMessage("Невозможно покинуть это мероприятие!")
                        builder.setPositiveButton("Понятно") { d, _ ->
                            d.dismiss()
                        }
                        val dialog = builder.create()
                        dialog.show()
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