package com.gy.ecotrace.repos

import com.gy.ecotrace.network.CalcApi
import com.gy.ecotrace.network.apiCall
import com.gy.ecotrace.network.dto.CalcAiSummary
import com.gy.ecotrace.network.dto.CalcAnswerInput
import com.gy.ecotrace.network.dto.CalcEntriesRequest
import com.gy.ecotrace.network.dto.CalcEntryResult
import com.gy.ecotrace.network.dto.CalcState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalcRepository @Inject constructor(
    private val api: CalcApi
) {
    suspend fun state(calculatorId: Int): CalcState = apiCall { api.state(calculatorId) }

    suspend fun submit(calculatorId: Int, answers: List<CalcAnswerInput>): CalcEntryResult =
        apiCall { api.submit(calculatorId, CalcEntriesRequest(answers)) }

    suspend fun aiSummary(calculatorId: Int): List<CalcAiSummary> =
        apiCall { api.aiSummary(calculatorId) }.items
}
