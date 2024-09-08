package com.gy.ecotrace.ui.more.events.createsteps

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.gy.ecotrace.R
import java.util.Calendar
import java.util.TimeZone

class CreateEventStep2: Fragment() {

    private val sharedViewModel: CreateEventViewModel by activityViewModels()

    private fun deleteLoadingFill(deleter: RelativeLayout): ValueAnimator {
        val maxLength = deleter.width / 2
        val loads = listOf(deleter.getChildAt(0), deleter.getChildAt(2))

        return ValueAnimator.ofInt(0, maxLength).apply {
            this.duration = 2000L
            this.interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                for (load in loads) {
                    val animatedValue = animation.animatedValue as Int
                    val layoutParams = load.layoutParams as ViewGroup.LayoutParams
                    layoutParams.width = animatedValue
                    load.layoutParams = layoutParams
                }
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) { deleter.tag = false }
                override fun onAnimationEnd(p0: Animator) { deleter.tag = true }
                override fun onAnimationCancel(p0: Animator) { deleter.tag = false }
                override fun onAnimationRepeat(p0: Animator) {}
            })
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activitylayout_create_event_step2, container, false)
    }
    private val allAddedTimes = mutableMapOf<String, String>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timesScrollView: ScrollView = view.findViewById(R.id.timesScrollViewCreateEvent)
        val timesLayout: LinearLayout = view.findViewById(R.id.timesLayoutCreateEvent)
        val addTime: Button = view.findViewById(R.id.addEventTime)

        fun addTimes() {
            if (allAddedTimes.isNotEmpty() && allAddedTimes.values.all { it.isEmpty() }) return
            val layout = layoutInflater.inflate(R.layout.layout_event_time, null)

            val descriptionEntry: EditText = layout.findViewById(R.id.timeDescription)
            val timeSetter: TextView = layout.findViewById(R.id.timeTime)
            val deleter: RelativeLayout = layout.findViewById(R.id.deleter)


            deleter.setOnTouchListener { view, motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        deleteLoadingFill(deleter).start()
                    }
                    else -> {
                        if (deleter.tag != null) {
                            if (layout.tag != null) {
                                Log.d("deleting", "values")
                                sharedViewModel.removeTime(layout.tag as String)
                                Toast.makeText(
                                    requireActivity(),
                                    "Время удалено!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            Log.d("deleting", "layout")
                            deleter.setOnTouchListener( null )
                            timesLayout.removeView(layout)
                            false
                        } else deleteLoadingFill(deleter).reverse()
                    }
                }
                true
            }

            fun createDatePickerDialog(
                context: Context, year: Int, month: Int, day: Int,
                minYear: Int? = null, minMonth: Int? = null, minDay: Int? = null,
                onDateSetListener: (Int, Int, Int) -> Unit
            ): DatePickerDialog {
                val picker = DatePickerDialog(
                    context,
                    DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
                        onDateSetListener(selectedYear, selectedMonth, selectedDay)
                    },
                    year,
                    month,
                    day
                )

                if (minDay != null) {
                    val calendar = Calendar.getInstance()
                    calendar.set(minYear!!, minMonth!!, minDay)
                    picker.datePicker.minDate = calendar.timeInMillis
                }

                return picker
            }

            fun createTimePickerDialog(
                context: Context, hour: Int, minute: Int,
                minHour: Int? = null, minMinute: Int? = null,
                year: Int? = null, month: Int? = null, day: Int? = null,
                minYear: Int? = null, minMonth: Int? = null, minDay: Int? = null,
                onTimeSetListener: (Int, Int) -> Unit
            ): TimePickerDialog {
                val picker = TimePickerDialog(
                    context,
                    TimePickerDialog.OnTimeSetListener { it, hourChosen, minuteChosen ->
                        if (minMinute != null) {
                            if (year == minYear && month == minMonth && day == minDay) {
                                if (hourChosen < minHour!! || (hourChosen == minHour && minuteChosen < minMinute)) {
                                    Log.d("bad time", "bad time, the future is the past")
                                    onTimeSetListener(minHour, minMinute)
                                } else {
                                    Log.d("bad time", "ok time, same day, but ok")
                                    onTimeSetListener(hourChosen, minuteChosen)
                                }
                            } else {
                                Log.d("bad time", "ok time")
                                onTimeSetListener(hourChosen, minuteChosen)
                            }
                        } else {
                            Log.d("bad time", "ok time, first pick")
                            onTimeSetListener(hourChosen, minuteChosen)
                        }

                    },
                    hour,
                    minute,
                    true
                )

                return picker
            }



            fun formatDateTime(
                day1: Int, month1: Int, year1: Int, hour1: Int, minute1: Int, // для textview
                day2: Int, month2: Int, year2: Int, hour2: Int, minute2: Int
            ): String {
                return String.format(
                    "%02d.%02d.%d %02d:%02d - %02d.%02d.%d %02d:%02d",
                    day1, month1 + 1, year1, hour1, minute1,
                    day2, month2 + 1, year2, hour2, minute2
                )
            }

            fun indexUTC(utc1: Long, utc2: Long): String { // для сохранения
                val calendar1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar1.timeInMillis = utc1
                val y1 = calendar1.get(Calendar.YEAR)
                val m1 = calendar1.get(Calendar.MONTH) + 1
                val d1 = calendar1.get(Calendar.DAY_OF_MONTH)
                val h1 = calendar1.get(Calendar.HOUR_OF_DAY)
                val mi1 = calendar1.get(Calendar.MINUTE)

                val calendar2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar2.timeInMillis = utc2
                val y2 = calendar2.get(Calendar.YEAR)
                val m2 = calendar2.get(Calendar.MONTH) + 1
                val d2 = calendar2.get(Calendar.DAY_OF_MONTH)
                val h2 = calendar2.get(Calendar.HOUR_OF_DAY)
                val mi2 = calendar2.get(Calendar.MINUTE)

                Log.d("testdate", "$y1 $y2")

                return String.format(
                    "%02d%02d%d%02d%02d_%d%02d%02d%02d%02d",
                    d1,
                    m1,
                    y1,
                    h1,
                    mi1,
                    d2,
                    m2,
                    y2,
                    h2,
                    mi2
                )
            }

            fun currentMillis(year: Int, month: Int, day: Int, hourOfDay: Int, minute: Int): Long {
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day, hourOfDay, minute)
                return calendar.timeInMillis
            }

            fun handleDateTimeSelection(view: View) {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                val datePickerDialog1 = createDatePickerDialog(
                    view.context,
                    year,
                    month,
                    day
                ) { selectedYear1, selectedMonth1, selectedDay1 ->
                    val timePickerDialog1 = createTimePickerDialog(
                        view.context, calendar.get(
                            Calendar.HOUR_OF_DAY
                        ), calendar.get(Calendar.MINUTE)
                    ) { hour1, minute1 ->

                        val millis1 = currentMillis(
                            selectedYear1,
                            selectedMonth1,
                            selectedDay1,
                            hour1,
                            minute1
                        )

                        val datePickerDialog2 = createDatePickerDialog(
                            view.context, year, month, day,
                            selectedYear1, selectedMonth1, selectedDay1
                        ) { selectedYear2, selectedMonth2, selectedDay2 ->
                            val timePickerDialog2 = createTimePickerDialog(
                                view.context,
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                hour1, minute1,
                                selectedYear2, selectedMonth2, selectedDay2,
                                selectedYear1, selectedMonth1, selectedDay1
                            ) { hourOfDay2, minute2 ->

                                val millis2 = currentMillis(
                                    selectedYear2,
                                    selectedMonth2,
                                    selectedDay2,
                                    hourOfDay2,
                                    minute2
                                )
                                if (millis1 < millis2) {
                                    timeSetter.text = formatDateTime(
                                        selectedDay1,
                                        selectedMonth1,
                                        selectedYear1,
                                        hour1,
                                        minute1,
                                        selectedDay2,
                                        selectedMonth2,
                                        selectedYear2,
                                        hourOfDay2,
                                        minute2
                                    )
                                    val utcDateIndex = indexUTC(millis1, millis2)
                                    layout.tag = utcDateIndex
                                    sharedViewModel.addTime(utcDateIndex, "")
                                } else {
                                    Toast.makeText(
                                        view.context,
                                        "Пожалуйста, выберите правильное время!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            }
                            Toast.makeText(
                                view.context,
                                "Выберите время окончания",
                                Toast.LENGTH_SHORT
                            ).show()
                            timePickerDialog2.show()
                        }
                        Toast.makeText(view.context, "Выберите дату окончания", Toast.LENGTH_SHORT)
                            .show()
                        datePickerDialog2.show()
                    }
                    Toast.makeText(view.context, "Выберите время начала", Toast.LENGTH_SHORT).show()
                    timePickerDialog1.show()
                }
                Toast.makeText(view.context, "Выберите дату начала", Toast.LENGTH_SHORT).show()
                datePickerDialog1.show()
            }

            descriptionEntry.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (layout.tag != null) {

                        sharedViewModel.addTime(layout.tag as String, s.toString())

                    } else {
                        Toast.makeText(
                            view.context,
                            "Пожалуйста, сначала установите время!",
                            Toast.LENGTH_LONG
                        ).show()
                        descriptionEntry.text.clear()
                    }
                }
            })
            timeSetter.setOnClickListener {
                handleDateTimeSelection(it)
            }

            timesLayout.addView(layout)
        }

        addTime.setOnClickListener {
            Toast.makeText(
                    view.context,
                    "Установленное время автоматически подстроится под часовые пояса других пользователей!",
                    Toast.LENGTH_LONG
                ).show()
            timesScrollView.visibility = View.VISIBLE
            addTimes()
        }
    }
}