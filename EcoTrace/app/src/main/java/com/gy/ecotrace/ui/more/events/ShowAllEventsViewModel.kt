package com.gy.ecotrace.ui.more.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.dto.EventBounds
import com.gy.ecotrace.network.dto.RemoteEvent
import com.gy.ecotrace.network.dto.UserEventItem
import com.gy.ecotrace.repos.EventRepository
import com.gy.ecotrace.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowAllEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _events = MutableLiveData<List<RemoteEvent>>()
    val events: LiveData<List<RemoteEvent>> get() = _events

    var filter: String? = null
    var from: String? = null
    var to: String? = null
    var name: String? = null
    var sortBy: String? = null
    var minMembers: Int? = null
    var maxMembers: Int? = null
    var hasMap: Boolean? = null
    var lat: Double? = null
    var lng: Double? = null
    var radiusKm: Double? = null
    private var after: Long? = null
    var foundAll = false
    var isUpdating = false

    fun search(reset: Boolean = true) {
        viewModelScope.launch {
            if (reset) { after = null; foundAll = false }
            val page = runCatching {
                eventRepository.list(
                    filter, from, to, after, name, sortBy,
                    minMembers, maxMembers, hasMap, lat, lng, radiusKm
                )
            }.getOrNull()
            isUpdating = false
            if (page == null) { _events.postValue(emptyList()); return@launch }
            foundAll = !page.hasMore
            page.items.lastOrNull()?.let { after = it.id }
            _events.postValue(page.items)
        }
    }

    private val _bounds = MutableLiveData<EventBounds>()
    val bounds: LiveData<EventBounds> get() = _bounds

    fun loadBounds() {
        viewModelScope.launch {
            _bounds.postValue(runCatching { eventRepository.bounds() }.getOrDefault(EventBounds()))
        }
    }

    private val _userExperience = MutableLiveData<Long?>()
    val userExperience: LiveData<Long?> get() = _userExperience

    fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.cachedCurrentUser()?.let { _userExperience.postValue(it.experience) }
            runCatching { userRepository.refreshCurrentUser() }
                .onSuccess { _userExperience.postValue(it.experience) }
        }
    }

    private val _myEvents = MutableLiveData<List<UserEventItem>>()
    val myEvents: LiveData<List<UserEventItem>> get() = _myEvents

    fun loadMyEvents(userId: Long) {
        viewModelScope.launch {
            _myEvents.postValue(runCatching { userRepository.getEvents(userId).items }.getOrDefault(emptyList()))
        }
    }

    fun leaveEvent(eventId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { eventRepository.leave(eventId) }.getOrDefault(false)
            callback(ok)
        }
    }
}
