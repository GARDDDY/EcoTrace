package com.gy.ecotrace.network.dto

data class CalcAverage(
    val question: Int = 0,
    val value: Double = 0.0,
    val count: Int = 0
)

data class CalcTodayEntry(
    val question: Int = 0,
    val value: Double = 0.0,
    val specify: Map<String, Double>? = null
)

data class CalcState(
    val updating: Boolean = false,   // true if already submitted today; new entries sum with today's
    val first: Boolean = false,      // true if user has never completed this calculator
    val daysSince: Int? = null,      // null if there's no data at all
    val averages: List<CalcAverage> = emptyList(),
    val today: List<CalcTodayEntry> = emptyList()
)

data class CalcAnswerInput(
    val value: Double,
    val specify: Map<String, Double>? = null
)

data class CalcEntriesRequest(
    val answers: List<CalcAnswerInput>
)

data class CalcEntryResult(
    val awarded: Int = 0,            // 0 if not the first submission of the day
    val message: String = ""
)

data class CalcSpecifyConfig(
    val available: List<String> = emptyList()
)

data class CalcSpecifySeries(
    val key: Int = 0,
    val name: String = "",
    val color: String = "128, 128, 128",
    val data: List<Double> = emptyList()
)

data class CalcSpecifyChart(
    val unit: String = "",
    val days: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val datasets: List<CalcSpecifySeries> = emptyList()
)
