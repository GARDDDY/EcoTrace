package com.gy.ecotrace.ui.more.ecocalculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.dto.CalcState
import com.gy.ecotrace.repos.CalcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalcCardUi(
    val calculatorId: Int,
    val state: CalcState? = null
)

@HiltViewModel
class CalcDashboardViewModel @Inject constructor(
    private val repository: CalcRepository
) : ViewModel() {

    private val _cards = MutableStateFlow<List<CalcCardUi>>(emptyList())
    val cards: StateFlow<List<CalcCardUi>> = _cards

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _loading.value = true
            val results = (0..4).map { id ->
                async { id to runCatching { repository.state(id) }.getOrNull() }
            }.awaitAll()
            _cards.value = results.map { (id, state) -> CalcCardUi(id, state) }
            _loading.value = false
        }
    }
}
