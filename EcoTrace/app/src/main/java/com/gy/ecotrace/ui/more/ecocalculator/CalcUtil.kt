package com.gy.ecotrace.ui.more.ecocalculator

import android.content.Context
import com.gy.ecotrace.R

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
