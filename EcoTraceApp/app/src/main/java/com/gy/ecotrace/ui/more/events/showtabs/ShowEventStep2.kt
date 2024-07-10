package com.gy.ecotrace.ui.more.events.showtabs

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.events.ShowEventViewModel
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.mapview.MapView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ShowEventStep2 : Fragment() {
    private val timeFormat: SimpleDateFormat = SimpleDateFormat("ddMMyyyyHHmm", Locale.getDefault())
    private val hoursMinutes: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val daysMonthsYears: SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val handler = Handler(Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activitylayout_show_event_step2, container, false)
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentEvent = Globals.getInstance().getString("CurrentlyWatchingEvent")
        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())
        val factory = ShowEventViewModelFactory(repository)
        val eventViewModel = ViewModelProvider(this, factory)[ShowEventViewModel::class.java]


        eventViewModel.getEventMore(currentEvent)
        eventViewModel.eventmore.observe(viewLifecycleOwner, Observer{
            it?.let {
                val eventGoalsLayout: LinearLayout = view.findViewById(R.id.planLayoutGoals)
                for (goal in it.eventGoals) {
                    val goalView = TextView(context)
                    goalView.textSize = 16F
                    goalView.text = Html.fromHtml(goal.value, Html.FROM_HTML_MODE_LEGACY)

                    eventGoalsLayout.addView(goalView)
                }

                val eventTimesLayout: LinearLayout = view.findViewById(R.id.planLayoutTimes)
                if (it.eventTimes != null) {
//                    startUpdatingColors(it.eventTimes, eventTimesLayout)
                } else {
                    view.findViewById<TextView>(R.id.NoTimesFound).visibility = View.VISIBLE
                    eventTimesLayout.visibility = View.GONE
                }
            }
        })
    }

    private fun parsePlannedTimePeriod(plannedTime: String): MutableList<Date> {
        val times = plannedTime.split("_")
        val startTimeUtc = times[0]
        val endTimeUtc = times[1]
        timeFormat.timeZone = TimeZone.getTimeZone("UTC")
        val startTime = timeFormat.parse(startTimeUtc)
        val endTime = timeFormat.parse(endTimeUtc)

        return mutableListOf(startTime!!, endTime!!)
    }

    private fun applyTimeColor(plannedTime: MutableList<Date>, currentTime: Date): Pair<String, String> {
        val timeString = "${hoursMinutes.format(plannedTime[0])}-${hoursMinutes.format(plannedTime[1])}"
        val dayStart = daysMonthsYears.format(plannedTime[0])
        val dayEnd = daysMonthsYears.format(plannedTime[1])
        var days = "${dayStart}-${dayEnd}"
        if (dayStart == dayEnd) days = dayStart
        return Pair(when {
            currentTime.after(plannedTime[0]) && currentTime.before(plannedTime[1]) -> {
                "<b><font color='${String.format("#%06X", 0xFFFFFF and
                        ContextCompat.getColor(requireContext(), R.color.ok_green))}'>$timeString</font></b>"
            }
            currentTime.after(plannedTime[1]) -> {
                "<b><font color='${String.format("#%06X", 0xFFFFFF and
                        ContextCompat.getColor(requireContext(), R.color.red_no))}'>$timeString</font></b>"
            }
            else -> {
                "<b>$timeString</b>"
            }
        }, days)
    }

    private fun getTime(plannedTimeUtc: MutableList<Date>): MutableList<Date> {
        val localTimes = mutableListOf<Date>()
        plannedTimeUtc.forEach { utcTime ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.time = utcTime
            calendar.timeZone = TimeZone.getDefault()
            localTimes.add(calendar.time)
        }
        return localTimes
    }


//    private fun startUpdatingColors(eventTimes: MutableMap<String, String>, eventTimesLayout: LinearLayout) {
//        handler.post(object : Runnable {
//            override fun run() {
//                eventTimesLayout.removeAllViews()
//                for (time in eventTimes.toSortedMap()) {
//                    val timeLinearLayout = LinearLayout(context)
//                    timeLinearLayout.orientation = LinearLayout.HORIZONTAL
//                    val timeViewName = TextView(context) // время
//                    timeViewName.textSize = 16F
//                    timeViewName.updatePadding(0,0,15,0)
//                    val data = applyTimeColor(getTime(parsePlannedTimePeriod(time.key)), Date())
//                    timeViewName.text = Html.fromHtml(data.first, Html.FROM_HTML_MODE_LEGACY)
//                    timeViewName.tooltipText = data.second
//                    val timeView = TextView(context) // описание
//                    timeView.textSize = 16F
//                    timeView.text = time.value
//                    timeView.setTextColor(Color.parseColor("#000000"))
//
//                    timeLinearLayout.addView(timeViewName)
//                    timeLinearLayout.addView(timeView)
//                    eventTimesLayout.addView(timeLinearLayout)
//                }
//                handler.postDelayed(this, 10000)
//            }
//        })
//    }
}