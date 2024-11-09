package com.gy.ecotrace.ui.more.ecocalculator

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.network.TokenStore
import com.gy.ecotrace.network.dto.CalcSpecifyChart
import com.gy.ecotrace.network.dto.GraphData
import com.gy.ecotrace.repos.CalcRepository
import com.gy.ecotrace.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpecifyBlockUi(
    val calculatorId: Int,
    val question: Int,
    val title: String,
    val chart: CalcSpecifyChart,
    val advices: List<String>
)

@HiltViewModel
class CalcResultsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val calcRepository: CalcRepository,
    private val tokenStore: TokenStore,
    @ApplicationContext private val context: Context
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

    private val _specifyBlocks = MutableLiveData<List<SpecifyBlockUi>>()
    val specifyBlocks: LiveData<List<SpecifyBlockUi>> get() = _specifyBlocks

    private val _specifyLoading = MutableLiveData(true)
    val specifyLoading: LiveData<Boolean> get() = _specifyLoading

    private var specifyLoaded = false

    fun loadSpecifyBlocks() {
        if (specifyLoaded) return
        specifyLoaded = true
        viewModelScope.launch {
            _specifyLoading.value = true
            val available = runCatching { calcRepository.specifyAvailable() }.getOrDefault(emptySet())

            val blocks = available.mapNotNull { key ->
                val parts = key.split("_")
                val calcId = parts.getOrNull(0)?.toIntOrNull()
                val question = parts.getOrNull(1)?.toIntOrNull()
                if (calcId == null || question == null) null else calcId to question
            }.map { (calcId, question) ->
                async {
                    val chart = runCatching { calcRepository.specify(calcId, question) }.getOrNull()
                    val advices = runCatching { calcRepository.advices(calcId, question) }.getOrDefault(emptyList())
                    val title = runCatching { questionTitle(calcId, question) }.getOrNull()
                        ?: context.getString(CALC_NAMES[calcId])
                    if (chart == null || chart.datasets.isEmpty()) null
                    else SpecifyBlockUi(calcId, question, title, chart, advices)
                }
            }.awaitAll().filterNotNull()

            _specifyBlocks.value = blocks
            _specifyLoading.value = false
        }
    }

    private fun questionTitle(calcId: Int, question: Int): String? {
        val fileName = "ecological-calculators/${calcId}1.json"
        val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val root = Gson().fromJson(json, JsonObject::class.java)
        val questions: HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion> = Gson().fromJson(
            root.getAsJsonObject("tests").toString(),
            object : TypeToken<HashMap<String, DatabaseMethods.DataClasses.EcoCalcQuestion>>() {}.type
        )
        return questions["${question + 1}"]?.qData
    }
}
