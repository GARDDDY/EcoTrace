package com.gy.ecotrace.ui.more.friends

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

class FriendsSortBottomSheet : DialogFragment() {

    companion object {
        const val TAG = "FriendsSortBottomSheet"
        const val RESULT_KEY = "friends_sort_result"
        const val ARG_SORT = "sort"

        fun newInstance(currentSort: String?): FriendsSortBottomSheet =
            FriendsSortBottomSheet().apply {
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
        inflater.inflate(R.layout.layout_friends__search_friends_sort_by, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sortOptions = view.findViewById<RadioGroup>(R.id.sortOptions)
        val applyBtn = view.findViewById<Button>(R.id.applySort)

        when (arguments?.getString(ARG_SORT)) {
            "rating" -> sortOptions.check(R.id.sortByRating)
            else -> sortOptions.check(R.id.sortDefault)
        }

        applyBtn.setOnClickListener {
            val sort = if (sortOptions.checkedRadioButtonId == R.id.sortByRating) "rating" else null
            setFragmentResult(RESULT_KEY, bundleOf(ARG_SORT to sort))
            dismiss()
        }

        view.setOnClickListener { dismiss() }
    }
}
