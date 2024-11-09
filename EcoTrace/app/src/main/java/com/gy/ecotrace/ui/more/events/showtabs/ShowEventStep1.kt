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
import com.gy.ecotrace.customs.ETAuth
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.events.createsteps.CreateEventViewModel
import com.yandex.mapkit.search.Line
import java.text.SimpleDateFormat
import java.util.Locale

class ShowEventStep1 : Fragment() {

    private val sharedViewModel: ShowEventViewModel by activityViewModels()
    private val currentUser = ETAuth.getInstance().getUID()
    private lateinit var currentEventCreator: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activitylayout_show_event_step1, container, false)
    }

    private fun reapplyJoinBtn(joinButton: Button, it: Boolean) {
        if (it) {
            joinButton.text = "В мероприятии"
            joinButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.ok_green))
            joinButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dirt_white))
            joinButton.setOnClickListener{
                val builder = android.app.AlertDialog.Builder(requireActivity())
                builder.setTitle("Выход из мероприятия")
                if (currentUser != currentEventCreator) {

                    builder.setMessage("Вы действительно хотите покинуть это мероприятие?")
                    builder.setPositiveButton(getString(R.string.confirm)) { dialog, which ->
                        sharedViewModel.leaveEvent {
                            reapplyJoinBtn(joinButton, it)
                        }
                    }
                    builder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                        dialog.dismiss()
                    }
                } else {
                    builder.setMessage("Вы не можете покинуть это мероприятие, так как являетесь его создателем! Для выхода из мероприятия надо его удалить!")
                    builder.setNeutralButton("Понятно") { dialog, which ->
                        dialog.dismiss()
                    }
                }
                val dialog = builder.create()
                dialog.show()

            }
        } else {
            joinButton.text = "Присоединиться"
            joinButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.dirt_white))
            joinButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ok_green))
            joinButton.setOnClickListener{
                sharedViewModel.joinEvent {
                    reapplyJoinBtn(joinButton, it)
                }
            }
        }
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

    private fun toLocalTime(ts: Long): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return dateFormat.format(ts*1000)
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
            currentEventCreator = event.eventCreatorId
            view.findViewById<TextView>(R.id.eventName).text = event.eventName
            view.findViewById<TextView>(R.id.eventAbout).text = event.eventAbout
            view.findViewById<TextView>(R.id.eventCountMembers).text = "${event.eventCountMembers} ${members(event.eventCountMembers)}"
            view.findViewById<TextView>(R.id.eventStartDate).text = when (event.eventStatus) {
                0 -> "Начнется с ${toLocalTime(event.minTime)}\nи продлится по ${toLocalTime(event.maxTime)}"
                1 -> "Проходит до ${toLocalTime(event.maxTime)}"
                2 -> "Проходило с ${toLocalTime(event.minTime)}\nпо ${toLocalTime(event.maxTime)}"
                else -> "???"
            }

            val tags: Array<Pair<String, String>> = Globals.getInstance().getEventsFilters()
            val colors: Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
            for (tag in try {event.filters.split(',').map { it.toInt()-1 }} catch (e: Exception) { listOf()}) {
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
                        sharedViewModel.leaveEvent {
                            reapplyJoinBtn(joinBtn, it)
                        }
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
                    sharedViewModel.joinEvent {
                        reapplyJoinBtn(joinBtn, it)
                    }
                }
            }
        })

        val allGoals: LinearLayout = view.findViewById(R.id.allGoalsLayout)

        sharedViewModel.getGoals()
        sharedViewModel.eventGoals.observe(viewLifecycleOwner, Observer {
            it?.let {
                allGoals.removeAllViews()
                for (goal in it.indices) {
                    val goalLayout = layoutInflater.inflate(R.layout.layout_event_goaltime, null)
                    goalLayout.findViewById<TextView>(R.id.objectName).text = "${goal+1}. ${Html.fromHtml(it[goal], Html.FROM_HTML_MODE_LEGACY)}"
                    allGoals.addView(goalLayout)
                }
            }
        })

    }
}