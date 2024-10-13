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
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.gy.ecotrace.R
import java.util.Calendar
import java.util.TimeZone

class CreateEventStep3: Fragment() {

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
        return inflater.inflate(R.layout.activitylayout_create_event_step3, container, false)
    }
    private val allAddedTimes = mutableMapOf<String, String>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timesLayout: LinearLayout = view.findViewById(R.id.goalLayoutCreateEvent)
        val addTime: Button = view.findViewById(R.id.addEventGoal)

        fun addGoals(id: String? = null, value: String? = null) {
            val layout = layoutInflater.inflate(R.layout.layout_event_goal, null)

            val descriptionEntry: EditText = layout.findViewById(R.id.goalText)
            val deleter: ImageButton = layout.findViewById(R.id.goalRemove)

            layout.tag = id ?: sharedViewModel.addGoal()
            if (value != null) descriptionEntry.setText(value)


            deleter.setOnClickListener {
                sharedViewModel.removeGoal(layout.tag as String)
                deleter.setOnClickListener { null }
                timesLayout.removeView(layout)
            }


            descriptionEntry.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (layout.tag != null) {
                        sharedViewModel.addGoal(layout.tag as String, s.toString())
                    }
                }
            })

            timesLayout.addView(layout)
        }

        sharedViewModel.getGoals()
        sharedViewModel.toAddeventGoals.observe(viewLifecycleOwner, Observer {
            for (i in it.indices) {
                sharedViewModel.addGoal("$i", it[i])
                addGoals("$i", it[i])
            }
        })

        addTime.setOnClickListener {
            addGoals()
        }
    }
}