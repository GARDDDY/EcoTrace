package com.gy.ecotrace.ui.news

import android.app.ActionBar.LayoutParams
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import com.gy.ecotrace.R
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


@RequiresApi(Build.VERSION_CODES.O)
class NewsFragment : Fragment() {
    private fun getTodayUtc(dayOffset: Long = 0): String {
        val utcDate = OffsetDateTime.now(ZoneOffset.UTC)
        val newDay = utcDate.minusDays(dayOffset)
        return newDay.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_news, container, false)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val datesDropdown: Spinner = view.findViewById(R.id.dropdown_select_dates)
        val datesArray = ArrayList<String>()
        for (i in 0..2) {
            datesArray.add(getTodayUtc(i.toLong()))
        }
//        val adapter = ArrayAdapter(requireContext(), R.layout.widget_custom_spinner_item, datesArray)
//        adapter.setDropDownViewResource(R.layout.widget_custom_spinner_dropdown_item)
//        datesDropdown.adapter = adapter
        val news_1: LinearLayout = view.findViewById(R.id.news_1)

        news_1.setOnClickListener{
            val mainLayout: ConstraintLayout = view.findViewById(R.id.news_root)
            val inflater = requireActivity().getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView: View = inflater.inflate(R.layout.layout_popup_news_full_info, null)
            mainLayout.post{
                val window = PopupWindow(popupView, mainLayout.width, mainLayout.height, true)
                window.showAtLocation(mainLayout, Gravity.TOP, 0, 0)

                // Добавить блюр

                val params = (popupView.layoutParams as ViewGroup.MarginLayoutParams)
                params.setMargins(80)

                popupView.findViewById<ImageButton>(R.id.popup_button).setOnClickListener { window.dismiss() }

                val dislikeSource = popupView.findViewById<ImageButton>(R.id.like_this_source_view_news)
                dislikeSource.setOnClickListener {
                    dislikeSource.setImageResource(R.drawable.baseline_heart_broken_24)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}