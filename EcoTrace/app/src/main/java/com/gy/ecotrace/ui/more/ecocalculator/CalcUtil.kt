package com.gy.ecotrace.ui.more.ecocalculator

import android.content.Context
import android.graphics.Color
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.gy.ecotrace.R
import com.gy.ecotrace.network.dto.CalcSpecifyChart
import com.gy.ecotrace.ui.more.profile.GraphMarkerView

// Индексы 0..4 совпадают с calculator.id на бэкенде и с именами файлов ассетов (${id}1.json).
val CALC_NAMES = intArrayOf(R.string.food, R.string.water, R.string.trash, R.string.energy, R.string.transport)
val CALC_ICONS = intArrayOf(
    R.drawable.baseline_fastfood_24,
    R.drawable.baseline_water_drop_24,
    R.drawable.baseline_compost_24,
    R.drawable.baseline_energy_savings_leaf_24,
    R.drawable.baseline_directions_car_24
)

fun dayWord(context: Context, days: Int): String = when {
    days % 10 == 1 && days % 100 != 11 -> context.getString(R.string.dayiped)
    days % 10 in 2..4 && (days % 100 < 10 || days % 100 >= 20) -> context.getString(R.string.dayrped)
    else -> context.getString(R.string.dayrpgetmn)
}

fun daysAgoText(context: Context, days: Int): String = "$days ${dayWord(context, days)}"

fun renderSpecifyChart(chart: LineChart, data: CalcSpecifyChart) {
    val sets = data.datasets.map { ds ->
        val entries = ds.data.mapIndexed { i, value -> Entry(i.toFloat(), value.toFloat()) }
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
        valueFormatter = IndexAxisValueFormatter(data.labels)
    }
    chart.marker = GraphMarkerView(chart.context, chart, data.unit)
    chart.isHighlightPerTapEnabled = true
    chart.invalidate()
    chart.animateY(700, Easing.EaseOutQuart)
}
