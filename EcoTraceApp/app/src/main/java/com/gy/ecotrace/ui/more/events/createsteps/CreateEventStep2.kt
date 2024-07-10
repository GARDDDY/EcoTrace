package com.gy.ecotrace.ui.more.events.createsteps

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.DragEvent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.LAYOUT_INFLATER_SERVICE
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.events.CreateEventActivity.Companion.mainlayout
import java.util.Calendar
import java.util.TimeZone
import kotlin.random.Random

class CreateEventStep2: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activitylayout_create_event_step2, container, false)
    }
    private val allAddedGoals = mutableListOf<String>()
    private val allAddedTimes = mutableMapOf<String, String>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val goalsScrollView: ScrollView = view.findViewById(R.id.goalsScrollViewCreateEvent)
        val goalsLayout: LinearLayout = view.findViewById(R.id.goalsLayoutCreateEvent)
        val addGoal: Button = view.findViewById(R.id.addEventGoal)

        val timesScrollView: ScrollView = view.findViewById(R.id.timesScrollViewCreateEvent)
        val timesLayout: LinearLayout = view.findViewById(R.id.timesLayoutCreateEvent)
        val addTime: Button = view.findViewById(R.id.addEventTime)

        var boldOpened = false
        var italicOpened = false
        var colorOpened = false
        var underscoreOpened = false
        var crossOpened = false

        fun updateGoals(text: String, needUpdate: Int?) {
            if (needUpdate == null) {
                allAddedGoals.add(text)
            } else {
                allAddedGoals[needUpdate] = text
            }

            val layout = LinearLayout(view.context)
            layout.orientation = LinearLayout.HORIZONTAL

            val num = TextView(view.context)
            num.text = "${allAddedGoals.size}."
            num.textSize = 20F
            num.setTextColor(ContextCompat.getColor(view.context, R.color.black))
            num.setPadding(0, 0, 20, 0)

            val description = TextView(view.context)
            description.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
            description.textSize = 20F
            description.setEms(15)
            description.setTextColor(ContextCompat.getColor(view.context, R.color.black))

            val move = ImageButton(view.context)
            move.setImageResource(R.drawable.baseline_drag_handle_24)
            move.setBackgroundResource(R.color.transparent)

            move.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val data = ClipData.newPlainText("", "")
                    val shadowBuilder = View.DragShadowBuilder(view.parent as View)
                    view.startDrag(data, shadowBuilder, view.parent, 0)
                    true
                } else {
                    false
                }
            }

            fun getIndex(y: Float, using: View): Int {
                for (i in 0..<goalsLayout.childCount) {
                    val child = goalsLayout.getChildAt(i)
                    if (y >= child.top && y <= child.bottom && child != using) {
                        val height = child.top + (child.bottom - child.top) / 2
                        if (y <= height) {
                            Log.d("drag", "higher")
                            return i
                        }
                        if (y > height) {
                            Log.d("drag", "lower")
                            return i + 1
                        }
                    }

                }
                return goalsLayout.indexOfChild(using)
            }

            fun drawBorders(y: Float) {
                for (i in 0..<goalsLayout.childCount) {
                    val child = goalsLayout.getChildAt(i)
                    if (y >= child.top && y <= child.bottom) {
                        child.setBackgroundResource(R.color.dirt2_white)
                    } else child.setBackgroundResource(R.color.transparent)
                }
            }

            fun updNums(indexes: MutableList<Int>) {
                for (index in indexes) {
                    for (i in index - 1..index + 1) {
                        if (goalsLayout.getChildAt(i) != null) {
                            ((goalsLayout.getChildAt(i) as LinearLayout).getChildAt(0) as TextView).text =
                                "${i + 1}."
                        }
                    }
                }
            }

            var chosenDelete = false
            goalsLayout.setOnDragListener { _, dragEvent ->
                when (dragEvent.action) {
                    DragEvent.ACTION_DRAG_STARTED -> true
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        val drag = dragEvent.localState as View
                        drag.setBackgroundResource(R.color.transparent)
                        chosenDelete = false
                        true
                    }

                    DragEvent.ACTION_DRAG_EXITED -> {
                        val drag = dragEvent.localState as View
                        drag.setBackgroundResource(R.color.red_no)
                        chosenDelete = true
                        true
                    }

                    DragEvent.ACTION_DRAG_LOCATION -> {
                        drawBorders(dragEvent.y) // выделение
                        true
                    }

                    DragEvent.ACTION_DROP -> {
                        val drag = dragEvent.localState as View
                        val prevIndex = goalsLayout.indexOfChild(drag)
                        val dragText = allAddedGoals[prevIndex]
                        allAddedGoals.removeAt(prevIndex)

                        val index = getIndex(dragEvent.y, drag)
                        goalsLayout.removeView(drag)

                        if (index >= goalsLayout.childCount) {
                            goalsLayout.addView(drag)
                            drag.visibility = View.VISIBLE
                            allAddedGoals.add(dragText)
                        } else {
                            goalsLayout.addView(drag, index)
                            drag.visibility = View.VISIBLE
                            allAddedGoals.add(index, dragText)
                        }
                        updNums(mutableListOf(index, prevIndex))
                        true
                    }

                    DragEvent.ACTION_DRAG_ENDED -> { // удаление
                        if (chosenDelete) {
                            val drag = dragEvent.localState as View
                            val prevIndex = goalsLayout.indexOfChild(drag)
                            goalsLayout.removeView(drag)
                            allAddedGoals.removeAt(prevIndex)
                        }
                        if (goalsLayout.childCount > 0) {
                            drawBorders(-1F)
                        } else {
                            goalsScrollView.visibility = View.GONE
                        }
                        true
                    }

                    else -> false
                }
            }

            layout.addView(num)
            layout.addView(description)
            layout.addView(move)

            goalsLayout.addView(layout)
        }

        fun goalAddMenu(editNum: Int? = null) {
            fun formats(editor: EditText, tag: String, opened: Boolean, additional: String = "") {
                val cursor = editor.selectionStart
                val bCursor = editor.text.substring(0, cursor)
                val aCursor = editor.text.substring(cursor)

                val newText = if (!opened) {
                    "$bCursor</$tag>$aCursor"
                } else {
                    "$bCursor<$tag$additional>$aCursor"
                }
                editor.setText(newText)
                val cursorOffset = tag.length + 2 + if (!opened) 1 else additional.length
                editor.setSelection(cursor + cursorOffset)
            }

            val inflater = view.context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView: View = inflater.inflate(R.layout.layout_create_custom_text, null)
            mainlayout.post {
                val window = PopupWindow(popupView, mainlayout.width, WRAP_CONTENT, true)
                window.showAtLocation(mainlayout, Gravity.CENTER, 0, mainlayout.height / 3)

                val editor = popupView.findViewById<EditText>(R.id.editTextGoal)
                val previewText = popupView.findViewById<TextView>(R.id.textPreviewGoal)
                previewText.setTextColor(ContextCompat.getColor(view.context, R.color.black))

                popupView.findViewById<ImageButton>(R.id.confirmGoal).setOnClickListener {
                    goalsScrollView.visibility = View.VISIBLE
                    updateGoals(editor.text.toString(), editNum)
                }

                editor.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        previewText.text = Html.fromHtml(s.toString(), Html.FROM_HTML_MODE_LEGACY)
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                })

                popupView.findViewById<Button>(R.id.boldtextfilteradd).setOnClickListener {
                    boldOpened = !boldOpened
                    formats(editor, "b", boldOpened)
                }

                popupView.findViewById<Button>(R.id.italictextfilteradd).setOnClickListener {
                    italicOpened = !italicOpened
                    formats(editor, "em", italicOpened)
                }

                popupView.findViewById<ImageButton>(R.id.colorchangefilteradd).setOnClickListener {
                    colorOpened = !colorOpened
                    val color = String.format(
                        "#%02X%02X%02X",
                        Random.nextInt(256),
                        Random.nextInt(256),
                        Random.nextInt(256)
                    )
                    formats(editor, "font", colorOpened, " color='$color'")
                }

                popupView.findViewById<Button>(R.id.underscoretextfilteradd).setOnClickListener {
                    underscoreOpened = !underscoreOpened
                    formats(editor, "u", underscoreOpened)
                }

                popupView.findViewById<Button>(R.id.crosstextfilteradd).setOnClickListener {
                    crossOpened = !crossOpened
                    formats(editor, "s", crossOpened)
                }
            }
        }

        fun addTimes() {
            if (allAddedTimes.isNotEmpty() && allAddedTimes.values.all { it.isEmpty() }) return
            val layout = LinearLayout(view.context)
            layout.orientation = LinearLayout.HORIZONTAL
            layout.updatePadding(0, 0, 0, 10)
            val dateTime = TextView(view.context)
            dateTime.textSize = 20F
            dateTime.setTextColor(ContextCompat.getColor(view.context, R.color.ok_green))
            dateTime.setHint("Нажмите")
            dateTime.setEms(8)
            dateTime.setHintTextColor(ContextCompat.getColor(view.context, R.color.silver))
            dateTime.updatePadding(0, 0, 0, 10)
            val dateName = EditText(view.context)
            dateName.setTextColor(ContextCompat.getColor(view.context, R.color.ok_green))
            dateName.setHintTextColor(ContextCompat.getColor(view.context, R.color.silver))
            dateName.setHint("Описание")
            dateName.setEms(10)
            var dbindex = ""
            val delBtn = ImageButton(view.context)
            delBtn.setImageResource(R.drawable.baseline_cancel_24)
            delBtn.setBackgroundResource(R.color.transparent)
            delBtn.setOnClickListener {
                allAddedTimes.remove(dbindex)
                timesLayout.removeView(layout)
                if (timesLayout.childCount == 0) {
                    timesScrollView.visibility = View.GONE
                }
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

            fun currentMillis(year: Int, month: Int, day: Int, hourOfDay: Int, minute: Int): Long {
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day, hourOfDay, minute)
                return calendar.timeInMillis
            }

            fun formatDateTime(
                day1: Int, month1: Int, year1: Int, hour1: Int, minute1: Int, // для textview
                day2: Int, month2: Int, year2: Int, hour2: Int, minute2: Int
            ): String {
                return String.format(
                    "%02d.%02d.%d %02d:%02d\n▼\n%02d.%02d.%d %02d:%02d",
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
                                    dateTime.text = formatDateTime(
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
                                    dbindex = utcDateIndex
                                    allAddedTimes[utcDateIndex] = ""
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

            dateName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (dbindex != "") {
                        allAddedTimes[dbindex] = s.toString()
                    } else {
                        Toast.makeText(
                            view.context,
                            "Пожалуйста, сначала установите время!",
                            Toast.LENGTH_LONG
                        ).show()
                        dateName.text.clear()
                    }
                }
            })
            dateTime.setOnClickListener {
                handleDateTimeSelection(it)
            }

            layout.addView(dateTime)
            layout.addView(dateName)
            layout.addView(delBtn)

            timesLayout.addView(layout)
        }

        addGoal.setOnClickListener {
            goalAddMenu()
        }
        addTime.setOnClickListener {
            if (timesScrollView.visibility == View.GONE) {
                Toast.makeText(
                    view.context,
                    "Установленное время автоматически подстроится под часовые пояса других пользователей!",
                    Toast.LENGTH_LONG
                ).show()
            }
            timesScrollView.visibility = View.VISIBLE
            addTimes()
        }
    }

    fun goalsReturn(): MutableList<String> {
        return allAddedGoals.filter { it.isNotEmpty() }.toMutableList()
    }
    fun timesReturn(): MutableMap<String, String> {
        return allAddedTimes.filterValues { it.isNotEmpty() }.toMutableMap()
    }
}