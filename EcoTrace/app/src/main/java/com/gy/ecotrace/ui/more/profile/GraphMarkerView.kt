package com.gy.ecotrace.ui.more.profile

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.gy.ecotrace.R
import kotlin.math.roundToLong

class GraphMarkerView(
    context: Context,
    private val chart: LineChart,
    private val unit: String
) : MarkerView(context, R.layout.layout_graph_marker) {

    private val titleView: TextView = findViewById(R.id.markerTitle)
    private val valueView: TextView = findViewById(R.id.markerValue)
    private val offset = MPPointF()

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) {
            super.refreshContent(e, highlight)
            return
        }
        val dataSet = chart.data?.getDataSetByIndex(highlight?.dataSetIndex ?: 0)
        val dayLabel = (chart.xAxis.valueFormatter as? IndexAxisValueFormatter)?.getFormattedValue(e.x)

        titleView.text = listOfNotNull(dataSet?.label, dayLabel?.takeIf { it.isNotBlank() })
            .joinToString(" · ")
        valueView.text = if (unit.isBlank()) formatValue(e.y) else "${formatValue(e.y)} $unit"

        super.refreshContent(e, highlight)
    }

    private fun formatValue(v: Float): String {
        val rounded = v.roundToLong()
        return if (rounded.toFloat() == v) rounded.toString() else String.format("%.2f", v)
    }

    override fun getOffset(): MPPointF {
        offset.x = -(width / 2f)
        offset.y = -height.toFloat() - 12f
        return offset
    }
}
