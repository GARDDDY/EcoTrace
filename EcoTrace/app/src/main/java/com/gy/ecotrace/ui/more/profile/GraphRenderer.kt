package com.gy.ecotrace.ui.more.profile

import android.graphics.Color
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.gy.ecotrace.network.dto.GraphData

// Разбивает точки серии на непрерывные отрезки по датам без данных, чтобы
// линия не "перепрыгивала" через дни без записей, а график распадался на части.
private fun splitIntoSegments(data: List<Double?>): List<List<Entry>> {
    val segments = mutableListOf<List<Entry>>()
    var current = mutableListOf<Entry>()
    data.forEachIndexed { i, value ->
        if (value != null) {
            current.add(Entry(i.toFloat(), value.toFloat()))
        } else if (current.isNotEmpty()) {
            segments.add(current)
            current = mutableListOf()
        }
    }
    if (current.isNotEmpty()) segments.add(current)
    return segments
}

fun renderUserGraph(chart: LineChart, graph: GraphData) {
    val sets = mutableListOf<ILineDataSet>()
    val legendEntries = mutableListOf<LegendEntry>()

    graph.datasets.forEach { ds ->
        val rgb = ds.color.split(",").mapNotNull { it.trim().toIntOrNull() }
        val lineColor = if (rgb.size == 3) Color.rgb(rgb[0], rgb[1], rgb[2]) else Color.GRAY

        splitIntoSegments(ds.data).forEach { segment ->
            sets.add(LineDataSet(segment, ds.name).apply {
                color = lineColor
                setCircleColor(lineColor)
                lineWidth = 2f
                circleRadius = 3f
                setDrawValues(false)
            })
        }
        legendEntries.add(LegendEntry(ds.name, Legend.LegendForm.LINE, 8f, 2f, null, lineColor))
    }

    chart.data = LineData(sets)
    chart.description.isEnabled = false
    chart.axisRight.isEnabled = false
    chart.legend.isEnabled = true
    chart.legend.setCustom(legendEntries)
    chart.xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        granularity = 1f
        axisMinimum = 0f
        axisMaximum = (graph.labels.size - 1).coerceAtLeast(0).toFloat()
        valueFormatter = IndexAxisValueFormatter(graph.labels)
    }
    chart.marker = GraphMarkerView(chart.context, chart, graph.unit)
    chart.isHighlightPerTapEnabled = true
    chart.invalidate()
    chart.animateY(700, Easing.EaseOutQuart)
}
