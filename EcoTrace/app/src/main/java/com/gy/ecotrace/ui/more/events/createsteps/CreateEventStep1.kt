package com.gy.ecotrace.ui.more.events.createsteps

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CreateEventStep1 : Fragment() {

    private val sharedViewModel: CreateEventViewModel by activityViewModels()
    private var prevKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_layout_event__create_step_1, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startBtn = view.findViewById<MaterialButton>(R.id.startTimeButton)
        val endBtn   = view.findViewById<MaterialButton>(R.id.endTimeButton)
        val fmt      = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        var startSec: Long? = null
        var endSec: Long?   = null

        fun commitTime() {
            prevKey?.let { sharedViewModel.removeTime(it) }
            val s = startSec ?: return
            val key = "${s}_${endSec ?: ""}"
            sharedViewModel.addTime(key, "")
            prevKey = key
            sharedViewModel.saveDraft()
        }

        fun pickDateTime(initialSec: Long?, onPicked: (Long) -> Unit) {
            val cal = Calendar.getInstance()
            initialSec?.let { cal.time = Date(it * 1000L) }
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                TimePickerDialog(requireContext(), { _, h, min ->
                    cal.set(Calendar.HOUR_OF_DAY, h)
                    cal.set(Calendar.MINUTE, min)
                    cal.set(Calendar.SECOND, 0)
                    onPicked(cal.timeInMillis / 1000L)
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        startBtn.setOnClickListener {
            pickDateTime(startSec) { sec ->
                startSec = sec
                startBtn.text = fmt.format(Date(sec * 1000L))
                commitTime()
            }
        }

        endBtn.setOnClickListener {
            pickDateTime(endSec) { sec ->
                endSec = sec
                endBtn.text = fmt.format(Date(sec * 1000L))
                commitTime()
            }
        }

        sharedViewModel.eventData.observe(viewLifecycleOwner, Observer { eventClass ->
            val eventNameEntry: EditText = view.findViewById(R.id.createEventName)
            val eventAboutEntry: EditText = view.findViewById(R.id.createEventAbout)
            val filtersLayout: ChipGroup = view.findViewById(R.id.chosenTags)
            filtersLayout.removeAllViews()

            eventNameEntry.setText(eventClass.eventName)
            eventAboutEntry.setText(eventClass.eventAbout)

            eventNameEntry.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if ((s?.length ?: 0) > 5) {
                        eventNameEntry.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        eventClass.eventName = s.toString()
                    } else {
                        eventNameEntry.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.baseline_do_not_disturb_24, 0
                        )
                        eventClass.eventName = ""
                    }
                    sharedViewModel.saveDraft()
                }
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })

            eventAboutEntry.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    eventClass.eventAbout = s.toString()
                    sharedViewModel.saveDraft()
                }
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })

            val timeKey = sharedViewModel.eventTimes.value?.keys?.firstOrNull()
            timeKey?.split("_")?.let { parts ->
                parts.getOrNull(0)?.toLongOrNull()?.let { sec ->
                    startSec = sec
                    startBtn.text = fmt.format(Date(sec * 1000L))
                    prevKey = timeKey
                }
                parts.getOrNull(1)?.toLongOrNull()?.let { sec ->
                    endSec = sec
                    endBtn.text = fmt.format(Date(sec * 1000L))
                }
            }

            fun currentTags(): MutableSet<String> =
                eventClass.filters.split(',').map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()

            val usedTags = currentTags()
            val tags   = Globals.getInstance().getEventsFilters()
            val colors = Globals.getInstance().getFiltersColors()

            for (dat in tags.indices) {
                val tagName = tags[dat].first
                val isActive = usedTags.contains(tagName)
                val chip = Chip(requireContext())
                chip.text = tagName
                chip.isCheckable = true
                chip.isChecked   = isActive
                chip.setTextColor(Color.parseColor(colors[dat].second))
                chip.chipStrokeColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(Color.parseColor(colors[dat].first), Color.TRANSPARENT)
                )
                chip.chipStrokeWidth = 4f * resources.displayMetrics.density
                chip.chipBackgroundColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(
                        Color.parseColor(colors[dat].first),
                        requireContext().getColor(R.color.transparent)
                    )
                )

                chip.setOnCheckedChangeListener { _, checked ->
                    val current = currentTags()
                    if (checked) current.add(tagName) else current.remove(tagName)
                    eventClass.filters = current.sorted().joinToString(",")
                    sharedViewModel.saveDraft()
                }
                filtersLayout.addView(chip)
            }

            eventClass.eventCountMembers = 1
        })
    }
}
