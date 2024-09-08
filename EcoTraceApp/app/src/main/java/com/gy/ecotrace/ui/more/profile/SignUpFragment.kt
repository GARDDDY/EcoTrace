package com.gy.ecotrace.ui.more.profile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.db.DatabaseMethods
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.random.Random

class SignUpFragment : Fragment() {

    private fun isOkEmail(string: String): Boolean {
        return Regex("\\w{4,}@\\w{3,}\\.\\w{2,}").matches(string)
    }
    private fun isOkPassword(string: String): Boolean {
        return string.length >= 6
    }

    private fun isOkLogin(string: String): Boolean {
        return Random.nextBoolean()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val replacerHub = activity as SignInUpHub

        // public

        val filtersLayout: LinearLayout = view.findViewById(R.id.userFiltersLayout)
        val tagsColors = DatabaseMethods.DataClasses.filterColors
        val allEventTags = DatabaseMethods.DataClasses.UserFiltersSearchBy
        for (tag in allEventTags.indices) {
            val tagButton = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
            tagButton.text = allEventTags[tag].first
            tagButton.textSize = 18F
            tagButton.setTextColor(Color.parseColor(tagsColors[tag].second))
            tagButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent))
            tagButton.rippleColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].second))
            tagButton.strokeColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].first))

            tagButton.setOnClickListener {
                tagButton.isActivated = !tagButton.isActivated
                if(!tagButton.isActivated) tagButton
                    .setBackgroundColor(ContextCompat
                        .getColor(requireContext(), R.color.transparent))
                else tagButton
                    .setBackgroundColor(Color.parseColor(tagsColors[tag].first))
            }

            filtersLayout.addView(tagButton)
        }

        val passwordEntry: EditText = view.findViewById(R.id.passwordData)
        val showPass: ImageButton = view.findViewById(R.id.showPasswordButton)
        showPass.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> { // hide password
                    showPass.setImageResource(R.drawable.eye_open)
                    passwordEntry.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT // Не скрытый текст
                }
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL -> {
                    showPass.setImageResource(R.drawable.eye_close)
                    passwordEntry.inputType = InputType.TYPE_CLASS_TEXT
                }
            }
            true
        }

        val emailEntry: EditText = view.findViewById(R.id.emailData)
        val loginEntry: EditText = view.findViewById(R.id.loginData)
        val fullnameEntry: EditText = view.findViewById(R.id.fullnameData)

        view.findViewById<Button>(R.id.registerButton).setOnClickListener {
            var criteria = 0
            if (isOkEmail(emailEntry.text.toString())) {
                emailEntry.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.ok_green)
                )
                criteria++
            } else emailEntry.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.red_no)
            )
            if (isOkPassword(passwordEntry.text.toString())) {
                passwordEntry.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.ok_green)
                )
                criteria++
            } else passwordEntry.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.red_no)
            )
            if (isOkLogin(loginEntry.text.toString())) {
                loginEntry.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.ok_green)
                )
                criteria++
            } else loginEntry.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.red_no)
            )

            if (criteria == 3) {
                Toast.makeText(requireActivity(), "Regged", Toast.LENGTH_LONG).show()
            }
        }
        view.findViewById<TextView>(R.id.gotoLogin).setOnClickListener {
            replacerHub.replaceFragment(SignInFragment())
        }

    }
}