package com.gy.ecotrace.ui.more.ecocalculator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.network.dto.CalcAnswerInput
import com.gy.ecotrace.network.dto.CalcState
import com.gy.ecotrace.network.dto.CalcTodayEntry
import com.gy.ecotrace.repos.CalcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyCalcViewModel @Inject constructor(
    private val repository: CalcRepository
) : ViewModel() {

    var currentPage = 0

    private var calculatorId = -1
    private var questionCount = -1
    private val answers: MutableMap<Int, DatabaseMethods.DataClasses.EcoCalcSaveData> = mutableMapOf()
    private var todayBaseline: Map<Int, CalcTodayEntry> = emptyMap()

    private val _state = MutableLiveData<CalcState?>()
    val state: LiveData<CalcState?> get() = _state

    private val _end = MutableLiveData<Boolean>()
    val end: LiveData<Boolean> get() = _end

    fun init(calculatorId: Int, questionCount: Int) {
        if (this.calculatorId == calculatorId) return
        this.calculatorId = calculatorId
        this.questionCount = questionCount
        viewModelScope.launch {
            val state = runCatching { repository.state(calculatorId) }.getOrNull()
            todayBaseline = state?.today?.associateBy { it.question } ?: emptyMap()
            _state.value = state
        }
    }

    fun addAnswer(value: DatabaseMethods.DataClasses.EcoCalcSaveData) {
        answers[value.question] = value
        _end.postValue(isEnoughAnswers())
    }

    fun hasAnswer(question: Int): Boolean = answers.containsKey(question)

    fun getAnswer(value: Int): DatabaseMethods.DataClasses.EcoCalcSaveData {
        return answers[value] ?: DatabaseMethods.DataClasses.EcoCalcSaveData(question = value)
    }

    fun todayBaselineFor(question: Int): CalcTodayEntry? = todayBaseline[question]

    fun isEnoughAnswers(): Boolean = answers.size == questionCount

    // Values sent to the server are deltas versus todayBaseline, not absolute totals.
    fun submit(callback: (String) -> Unit) {
        val ordered = (0 until questionCount).map { i ->
            val a = getAnswer(i)
            val baseline = todayBaseline[i]
            if (a.useSpecify && a.specify.isNotEmpty()) {
                val baselineSpecify = baseline?.specify ?: emptyMap()
                val deltaSpecify = a.specify.mapValues { (key, v) -> v - (baselineSpecify[key] ?: 0.0) }
                CalcAnswerInput(value = deltaSpecify.values.sum(), specify = deltaSpecify)
            } else {
                CalcAnswerInput(value = a.value.toDouble() - (baseline?.value ?: 0.0))
            }
        }
        viewModelScope.launch {
            val result = runCatching { repository.submit(calculatorId, ordered) }
            callback(result.getOrNull()?.message ?: "Произошла ошибка")
        }
    }
}
