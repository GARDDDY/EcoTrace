package com.gy.ecotrace.ui.more.friends

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R

class FriendsFiltersFragment : Fragment() {

    companion object {
        const val TAG = "FriendsFiltersFragment"
        const val RESULT_KEY = "friends_filters_result"
        const val ARG_FILTER = "filter"
        const val ARG_COMMON_GROUP = "common_group"
        const val ARG_COMMON_EVENT = "common_event"

        fun newInstance(filter: String?, commonGroup: Boolean?, commonEvent: Boolean?): FriendsFiltersFragment =
            FriendsFiltersFragment().apply {
                arguments = bundleOf(
                    ARG_FILTER to filter,
                    ARG_COMMON_GROUP to (commonGroup == true),
                    ARG_COMMON_EVENT to (commonEvent == true)
                )
            }
    }

    private var selectedFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedFilter = arguments?.getString(ARG_FILTER)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_friends_search_filters, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar8)
        val chipGroup = view.findViewById<ChipGroup>(R.id.tagsChipGroup)
        val commonGroupChip = view.findViewById<Chip>(R.id.commonGroupChip)
        val commonEventChip = view.findViewById<Chip>(R.id.commonEventChip)
        val applyBtn = view.findViewById<Button>(R.id.applyFiltersButton)

        toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        commonGroupChip.isChecked = arguments?.getBoolean(ARG_COMMON_GROUP) == true
        commonEventChip.isChecked = arguments?.getBoolean(ARG_COMMON_EVENT) == true

        val tags = Globals.getInstance().getUserFilters()
        val colors = Globals.getInstance().getFiltersColors()
        for (i in tags.indices) {
            val chip = Chip(requireContext()).apply {
                text = tags[i].first
                isCheckable = true
                chipStrokeWidth = 2f
                chipStrokeColor = ColorStateList.valueOf(Color.parseColor(colors[i].first))
                setTextColor(Color.parseColor(colors[i].second))
                chipBackgroundColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(Color.parseColor(colors[i].first), Color.TRANSPARENT)
                )
                isChecked = (tags[i].first == selectedFilter)
            }
            chip.setOnCheckedChangeListener { _, checked ->
                selectedFilter = if (checked) tags[i].first else null
            }
            chipGroup.addView(chip)
        }

        applyBtn.setOnClickListener {
            setFragmentResult(RESULT_KEY, bundleOf(
                ARG_FILTER to selectedFilter,
                ARG_COMMON_GROUP to commonGroupChip.isChecked,
                ARG_COMMON_EVENT to commonEventChip.isChecked
            ))
            parentFragmentManager.popBackStack()
        }
    }
}
