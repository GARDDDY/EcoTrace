package com.gy.ecotrace.sections.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.RatingApi
import com.gy.ecotrace.network.apiCall
import com.gy.ecotrace.network.dto.RatingItem
import com.gy.ecotrace.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RatingViewModel @Inject constructor(
    private val api: RatingApi,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<RatingItem>>(emptyList())
    val users: StateFlow<List<RatingItem>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _myCountryCode = MutableStateFlow<String?>(null)
    val myCountryCode: StateFlow<String?> = _myCountryCode

    // null means the "world" tab; otherwise holds the current country tab's code
    private var currentCountry: String? = null
    private var offset = 0

    var hasMore = true
        private set
    var isUpdating = false
        private set

    init {
        viewModelScope.launch {
            _myCountryCode.value = userRepository.cachedCurrentUser()?.countryCode
            runCatching { userRepository.refreshCurrentUser() }
                .onSuccess { _myCountryCode.value = it.countryCode }
        }
        loadWorld()
    }

    fun loadWorld() = switchTo(null)

    fun loadMyCountry() = switchTo(_myCountryCode.value)

    private fun switchTo(country: String?) {
        currentCountry = country
        offset = 0
        hasMore = true
        _users.value = emptyList()
        viewModelScope.launch {
            _isLoading.value = true
            fetchPage()
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (isUpdating || !hasMore) return
        isUpdating = true
        viewModelScope.launch {
            fetchPage()
            isUpdating = false
        }
    }

    private suspend fun fetchPage() {
        val page = runCatching { apiCall { api.rating(currentCountry, offset = offset) } }.getOrNull()
        if (page == null) {
            hasMore = false
            return
        }
        _users.value = _users.value + page.items
        offset += page.items.size
        hasMore = page.hasMore
    }
}
