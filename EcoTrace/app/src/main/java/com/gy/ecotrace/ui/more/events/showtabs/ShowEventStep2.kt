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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ShowEventStep2 : Fragment() {

    private val sharedViewModel: ShowEventViewModel by activityViewModels()

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

        sharedViewModel.getTimes()
        sharedViewModel.eventTimes.observe(viewLifecycleOwner, Observer {
            it?.let {
                startUpdatingColors(view.findViewById(R.id.planLayoutTimes))
            }
        })
    }

    private fun parseTime(timeString: String): String {
        val end = mutableListOf<String>()
        for (timeStamp in timeString.split('_')) {
            val timestampInMillis = timeStamp.toLong() * 1000
            val date = Date(timestampInMillis)
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            end.add(sdf.format(date))
        }
        return "${end[0]} - ${end[1]}"
    }

    private fun applyTimeColor(plannedTime: String, currentTime: Long): String {
        val dayStart = plannedTime.split('_')[0].toLong()
        val dayEnd = plannedTime.split('_')[1].toLong()
        return if (currentTime in dayStart..dayEnd) {
                "<b><font color='${String.format("#%06X", 0xFFFFFF and
                        ContextCompat.getColor(requireContext(), R.color.ok_green))}'>${parseTime(plannedTime)}</font></b>"
            }
            else if (dayEnd < currentTime) {
                "<b><font color='${String.format("#%06X", 0xFFFFFF and
                        ContextCompat.getColor(requireContext(), R.color.red_no))}'>" +
                        "${parseTime(plannedTime)}</font></b>"
            }
            else {
                "<b>${parseTime(plannedTime)}</b>"
            }
    }


    private fun startUpdatingColors(eventTimesLayout: LinearLayout) {
        handler.post(object : Runnable {
            override fun run() {
                eventTimesLayout.removeAllViews()
                for ((time, description) in sharedViewModel.eventTimes.value?.toSortedMap() ?: hashMapOf()) {
                    val timeLayout = layoutInflater.inflate(R.layout.layout_event_goaltime, null)
                    val timeData = applyTimeColor(time, System.currentTimeMillis()/1000)
                    val oData = timeLayout.findViewById<TextView>(R.id.objectData)
                    oData.visibility = View.VISIBLE
                    oData.text = Html.fromHtml(timeData, Html.FROM_HTML_MODE_LEGACY)

                    timeLayout.findViewById<TextView>(R.id.objectName).text = description

                    eventTimesLayout.addView(timeLayout)
                }
                handler.postDelayed(this, 40000)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}