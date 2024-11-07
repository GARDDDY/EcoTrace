package com.gy.ecotrace.ui.more.profile.sign

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.events.showtabs.ShowEventViewModel
import kotlin.math.log
import kotlin.random.Random

class SignUpFragment : Fragment() {

    private fun isOkEmail(string: String): Boolean {
        return true
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

    private val sharedViewModel: SignHubViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val replacerHub = activity as SignInUpHub

        // public

        val filtersLayout: LinearLayout = view.findViewById(R.id.userFiltersLayout)
        val tagsColors: Array<Pair<String, String>> = Globals.getInstance().getFiltersColors()
        val allUserTags: Array<Pair<String, String>> = Globals.getInstance().getUserFilters()
        for (tag in allUserTags.indices) {
            val tagButton = layoutInflater.inflate(R.layout.widget_tag_filter_button, null) as MaterialButton
            tagButton.text = allUserTags[tag].first
            tagButton.textSize = 18F
            tagButton.setTextColor(Color.parseColor(tagsColors[tag].second))
            tagButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent))
            tagButton.rippleColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].second))
            tagButton.strokeColor = ColorStateList.valueOf(Color.parseColor(tagsColors[tag].first))

            tagButton.setOnClickListener {
                val prevFilters = try {
                    sharedViewModel.user.filters!!.split(",").map { it.toInt() }.toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
                tagButton.isActivated = !tagButton.isActivated
                if (!tagButton.isActivated) {
                    tagButton
                        .setBackgroundColor(
                            ContextCompat
                                .getColor(requireContext(), R.color.transparent)
                        )
                    prevFilters.remove(tag+1)
                } else {
                    tagButton.setBackgroundColor(Color.parseColor(tagsColors[tag].first))
                    prevFilters.add(tag+1)
                    prevFilters.sort()
                }
                sharedViewModel.user.filters = try {prevFilters.joinToString { "," }} catch (e: Exception) { null }
            }

            filtersLayout.addView(tagButton)
        }

        val passwordEntry: TextInputEditText = view.findViewById(R.id.passwordData)


        val emailEntry: EditText = view.findViewById(R.id.emailData)
        val loginEntry: EditText = view.findViewById(R.id.usernameData)
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
                sharedViewModel.user.username = loginEntry.text.toString()
                criteria++
            } else loginEntry.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.red_no)
            )

//            if (criteria == 3) {
                sharedViewModel.user.fullname = fullnameEntry.text.toString()
                sharedViewModel.signUp(emailEntry.text.toString(), passwordEntry.text.toString()) {
                    if (it) {
                        requireActivity().runOnUiThread {
                            requireActivity().recreate()
                        }
                    }
                }
//            }
        }
        view.findViewById<TextView>(R.id.gotoLogin).setOnClickListener {
            replacerHub.replaceFragment(SignInFragment())
        }

    }
}