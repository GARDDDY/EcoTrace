package com.gy.ecotrace.ui.more.profile.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.facebook.shimmer.ShimmerFrameLayout
import com.github.mikephil.charting.charts.LineChart
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.profile.ProfileViewModel
import com.gy.ecotrace.ui.more.profile.renderUserGraph

class ProfileTabCalculator : Fragment() {

    private val viewModel: ProfileViewModel by activityViewModels()
    private val chipIds = listOf(R.id.chipFood, R.id.chipWater, R.id.chipTrash, R.id.chipEnergy, R.id.chipTransport)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_layout_profile__ecological_calculator, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chart = view.findViewById<LineChart>(R.id.userEcoDataGraph)
        val touchSlop = android.view.ViewConfiguration.get(requireContext()).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var claimedByChart = false
        chart.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    claimedByChart = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!claimedByChart) {
                        val dx = kotlin.math.abs(event.x - downX)
                        val dy = kotlin.math.abs(event.y - downY)
                        if (event.pointerCount > 1 || (dx > touchSlop && dx > dy)) {
                            claimedByChart = true
                            v.parent.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    claimedByChart = false
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        val periodToggle = view.findViewById<MaterialButtonToggleGroup>(R.id.graphPeriodToggle)
        val typeChips = view.findViewById<ChipGroup>(R.id.graphTypesChipGroup)

        periodToggle.check(if (viewModel.lastGraphTime == 1) R.id.period30Button else R.id.period7Button)
        viewModel.lastGraphTypes?.forEach { excludedIndex ->
            typeChips.findViewById<Chip>(chipIds[excludedIndex]).isChecked = false
        }
        if (viewModel.lastGraphTypes == null) {
            viewModel.lastGraphTypes = excludedTypes(typeChips)
        }

        fun reload() {
            showLoading(view)
            viewModel.graphReq = true
            viewModel.loadGraph(viewModel.currentUserId, viewModel.lastGraphTime, viewModel.lastGraphTypes)
        }

        periodToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val time = if (checkedId == R.id.period30Button) 1 else 0
            if (viewModel.lastGraphTime == time) return@addOnButtonCheckedListener
            viewModel.lastGraphTime = time
            reload()
        }
        typeChips.setOnCheckedStateChangeListener { group, _ ->
            viewModel.lastGraphTypes = excludedTypes(group as ChipGroup)
            reload()
        }

        viewModel.loadGraph(viewModel.currentUserId, viewModel.lastGraphTime, viewModel.lastGraphTypes)
        viewModel.graph.observe(viewLifecycleOwner, Observer { graph ->
            if (viewModel.graphReq) return@Observer
            view.findViewById<ShimmerFrameLayout>(R.id.userEcoDataInformationLayoutLoading).visibility = View.GONE
            val hasData = graph != null && graph.datasets.isNotEmpty()
            view.findViewById<TextView>(R.id.noEcoDataWarning).visibility = if (hasData) View.GONE else View.VISIBLE
            view.findViewById<RelativeLayout>(R.id.chartCard).visibility = if (hasData) View.VISIBLE else View.GONE
            if (hasData) renderUserGraph(chart, graph!!)
        })
    }

    private fun excludedTypes(chipGroup: ChipGroup): MutableList<Int> = chipIds
        .mapIndexedNotNull { index, id -> index.takeIf { !chipGroup.findViewById<Chip>(id).isChecked } }
        .toMutableList()

    private fun showLoading(view: View) {
        view.findViewById<ShimmerFrameLayout>(R.id.userEcoDataInformationLayoutLoading).visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.noEcoDataWarning).visibility = View.GONE
        view.findViewById<RelativeLayout>(R.id.chartCard).visibility = View.GONE
    }
}
