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
    val updating: Boolean = false,
    val first: Boolean = false,
    val daysSince: Int? = null,
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
    val awarded: Int = 0,
    val message: String = ""
)

data class CalcAiSummary(
    val period: Int = 7,
    val summary: String? = null,
    val generatedFor: String? = null
)

data class CalcAiSummaries(
    val items: List<CalcAiSummary> = emptyList()
)
