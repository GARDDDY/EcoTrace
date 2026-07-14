package com.gy.ecotrace.ui.more.ecocalculator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.network.dto.GraphData
import com.gy.ecotrace.repos.CalcRepository
import com.gy.ecotrace.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiSummaryUi(
    val calculatorId: Int,
    val period: Int,
    val summary: String
)

@HiltViewModel
class CalcResultsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calcRepository: CalcRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    var lastGraphTime = 0
    var lastGraphTypes: MutableList<Int>? = null

    private val _graph = MutableLiveData<GraphData?>()
    val graph: LiveData<GraphData?> get() = _graph

    private val _graphLoading = MutableLiveData(true)
    val graphLoading: LiveData<Boolean> get() = _graphLoading

    fun loadGraph(time: Int = lastGraphTime, excludeTypes: List<Int>? = lastGraphTypes) {
        lastGraphTime = time
        lastGraphTypes = excludeTypes?.toMutableList()
        viewModelScope.launch {
            _graphLoading.value = true
            _graph.value = runCatching { userRepository.getGraph(tokenStore.userId, time, excludeTypes) }.getOrNull()
            _graphLoading.value = false
        }
    }

    private val _aiSummaries = MutableLiveData<List<AiSummaryUi>>()
    val aiSummaries: LiveData<List<AiSummaryUi>> get() = _aiSummaries

    private val _aiSummariesLoading = MutableLiveData(true)
    val aiSummariesLoading: LiveData<Boolean> get() = _aiSummariesLoading

    private var aiSummariesLoaded = false

    fun loadAiSummaries() {
        if (aiSummariesLoaded) return
        aiSummariesLoaded = true
        viewModelScope.launch {
            _aiSummariesLoading.value = true
            val results = (0..4).map { id ->
                async { id to runCatching { calcRepository.aiSummary(id) }.getOrNull().orEmpty() }
            }.awaitAll()
            _aiSummaries.value = results.flatMap { (id, summaries) ->
                summaries.mapNotNull { s -> s.summary?.let { AiSummaryUi(id, s.period, it) } }
            }
            _aiSummariesLoading.value = false
        }
    }
}
