package com.gy.ecotrace.ui.more.profile

import android.graphics.Color
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.gy.ecotrace.network.dto.GraphData

fun renderUserGraph(chart: LineChart, graph: GraphData) {
    val sets = graph.datasets.map { ds ->
        val entries = ds.data.mapIndexedNotNull { i, value ->
            value?.let { Entry(i.toFloat(), it.toFloat()) }
        }
        LineDataSet(entries, ds.name).apply {
            val rgb = ds.color.split(",").mapNotNull { it.trim().toIntOrNull() }
            val lineColor = if (rgb.size == 3) Color.rgb(rgb[0], rgb[1], rgb[2]) else Color.GRAY
            color = lineColor
            setCircleColor(lineColor)
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
        }
    }

    chart.data = LineData(sets)
    chart.description.isEnabled = false
    chart.axisRight.isEnabled = false
    chart.legend.isEnabled = true
    chart.xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        granularity = 1f
        valueFormatter = IndexAxisValueFormatter(graph.labels)
    }
    chart.marker = GraphMarkerView(chart.context, chart, graph.unit)
    chart.isHighlightPerTapEnabled = true
    chart.invalidate()
    chart.animateY(700, Easing.EaseOutQuart)
}
