package com.gy.ecotrace.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.NetworkMonitor
import com.gy.ecotrace.repos.ServerStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerStateViewModel @Inject constructor(
    private val repository: ServerStateRepository
) : ViewModel() {

    val hasConnection: StateFlow<Boolean> = NetworkMonitor.isOnline

    init {
        checkConnectionLoop()
    }

    private fun checkConnectionLoop() {
        viewModelScope.launch {
            var consecutiveFailures = 0
            while (true) {
                val ok = repository.pingServer()
                if (ok) {
                    consecutiveFailures = 0
                    NetworkMonitor.update(true)
                    delay(ONLINE_PING_INTERVAL_MS)
                } else {
                    consecutiveFailures++
                    if (consecutiveFailures >= OFFLINE_FAILURE_THRESHOLD) {
                        NetworkMonitor.update(false)
                    }
                    delay(OFFLINE_PING_INTERVAL_MS)
                }
            }
        }
    }

    companion object {
        private const val ONLINE_PING_INTERVAL_MS = 30_000L
        private const val OFFLINE_PING_INTERVAL_MS = 5_000L
        private const val OFFLINE_FAILURE_THRESHOLD = 2
    }
}
