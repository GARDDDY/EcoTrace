package com.gy.ecotrace.ui.more.events

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.RadioGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.gy.ecotrace.R

class EventsSortBottomSheet : DialogFragment() {

    companion object {
        const val TAG = "EventsSortBottomSheet"
        const val RESULT_KEY = "sort_result"
        const val ARG_SORT = "sort"

        fun newInstance(currentSort: String?): EventsSortBottomSheet =
            EventsSortBottomSheet().apply {
                arguments = bundleOf(ARG_SORT to currentSort)
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        super.onCreateDialog(savedInstanceState).also { dialog ->
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog?.setCanceledOnTouchOutside(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_event__search_events_sort_by, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sortOptions = view.findViewById<RadioGroup>(R.id.sortOptions)
        val applyBtn = view.findViewById<Button>(R.id.applySort)

        when (arguments?.getString(ARG_SORT)) {
            "date_asc"     -> sortOptions.check(R.id.sortSoon)
            "date_desc"    -> sortOptions.check(R.id.sortNotSoon)
            "members_desc" -> sortOptions.check(R.id.sortMorePeople)
            "members_asc"  -> sortOptions.check(R.id.sortLessPeople)
        }

        applyBtn.setOnClickListener {
            val sort = when (sortOptions.checkedRadioButtonId) {
                R.id.sortSoon        -> "date_asc"
                R.id.sortNotSoon     -> "date_desc"
                R.id.sortMorePeople  -> "members_desc"
                R.id.sortLessPeople  -> "members_asc"
                else                 -> null
            }
            setFragmentResult(RESULT_KEY, bundleOf(ARG_SORT to sort))
            dismiss()
        }

        view.setOnClickListener { dismiss() }
    }
}
