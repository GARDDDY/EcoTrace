package com.gy.ecotrace.ui.more.events.createsteps

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods

class CreateEventStep1: Fragment() {

    private val sharedViewModel: CreateEventViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activitylayout_create_event_step1, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.eventData.observe(viewLifecycleOwner, Observer { eventClass ->

            val eventNameEntry: EditText = view.findViewById(R.id.createEventName)
            val eventAboutEntry: EditText = view.findViewById(R.id.createEventAbout)

            val filtersLayout: LinearLayout = view.findViewById(R.id.chosenTags)

            val startSpinner: Spinner = view.findViewById(R.id.chooseStartWay)
            val autostartMembers: EditText = view.findViewById(R.id.autostartWhenMembers)

            eventNameEntry.setText(eventClass.eventName)
            eventAboutEntry.setText(eventClass.eventAbout)

            eventNameEntry.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().length > 5) {
                        eventNameEntry.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
//                        sharedViewModel.applyEventData(eventName = s.toString())
                        eventClass.eventName = s.toString()
                    } else {
                        eventNameEntry.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0,
                            R.drawable.baseline_do_not_disturb_24, 0
                        )
                        eventClass.eventName = ""
                    }
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })

            eventAboutEntry.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable?) {
                    eventClass.eventAbout = s.toString()
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })

            startSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(selection: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    autostartMembers.visibility = View.GONE
                    val item = selection!!.selectedItemId
                    if (item == 1L) autostartMembers.visibility = View.VISIBLE
                    eventClass.eventStart = "$item;0"
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    eventClass.eventStart = "0;0"
                }
            }
            autostartMembers.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    eventClass.eventStart = "1;$s"
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })
            val usedTags = try {
                eventClass.filters.split(',').map { it.toInt()-1 }.toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            val tags: Array<Pair<String, String>> = Globals.getInstance().getEventsFilters()
            val colors: Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
            for (dat in tags.indices) {
                val layout = LinearLayout(context)
                layout.orientation = LinearLayout.HORIZONTAL
                val layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layout.layoutParams = layoutParams

                val isActive = usedTags.contains(dat)

                val filter = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
                filter.text = tags[dat].first
                filter.textSize = 18F
                filter.setTextColor(Color.parseColor(colors[dat].second))
                filter.setBackgroundColor(
                    if (isActive)
                        Color.parseColor(colors[dat].first)
                    else ContextCompat.getColor(requireContext(),R.color.transparent)
                )
                filter.isActivated = isActive
                filter.rippleColor = ColorStateList.valueOf(Color.parseColor(colors[dat].second))
                filter.isClickable = true
                val description = TextView(context)
                description.text = tags[dat].second
                description.textSize = 18F
                description.setEms(25)
                description.setTextColor(Color.BLACK)
                description.updatePadding(10,0,0,0)
                description.textAlignment = View.TEXT_ALIGNMENT_CENTER
                description.gravity = Gravity.TOP
                val textParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                description.layoutParams = textParams
                layout.addView(filter)
                layout.addView(description)

                filter.setOnClickListener {
                    filter.isActivated = !filter.isActivated
                    if(!filter.isActivated) filter.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent))
                    else filter.setBackgroundColor(Color.parseColor(colors[dat].first))
                }

                val startData = eventClass.eventStart.split(';').map { it.toInt() }
                Log.d("sd", startData.toString())
                startSpinner.setSelection(startData[0])
                if (startData[0] == 1) {
                    autostartMembers.visibility = View.VISIBLE
                    autostartMembers.setText(startData[1].toString())
                }



                filtersLayout.addView((layout))

            }


            eventClass.eventCountMembers = 1
//            eventClass.eventUsersToTheirRoles = hashMapOf()
//            eventClass.eventUsersToTheirRoles!![Globals.getInstance().getString("CurrentlyLogged")] = 0
        })


    }

}