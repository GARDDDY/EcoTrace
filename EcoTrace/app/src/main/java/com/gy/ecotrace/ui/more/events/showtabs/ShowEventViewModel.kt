package com.gy.ecotrace.ui.more.events.showtabs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.dto.EventGoal
import com.gy.ecotrace.network.dto.EventMember
import com.gy.ecotrace.network.dto.EventScheduleItem
import com.gy.ecotrace.network.dto.EventZone
import com.gy.ecotrace.network.dto.RemoteEvent
import com.gy.ecotrace.repos.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowEventViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    var currentEvent: Long = 0

    private val _event = MutableLiveData<RemoteEvent?>()
    val event: LiveData<RemoteEvent?> get() = _event

    val myRole: String? get() = _event.value?.myRole
    val isMember: Boolean get() = myRole != null
    val isCreator: Boolean get() = myRole == "creator"
    val isModer: Boolean get() = myRole == "creator" || myRole == "assistant"
    val isValidated: Boolean get() = _event.value?.myValidated == true

    private var collectingEvent = false

    fun getEvent() {
        if (!collectingEvent) {
            collectingEvent = true
            viewModelScope.launch { repository.observeEvent(currentEvent).collect { _event.postValue(it) } }
        }
        viewModelScope.launch { runCatching { repository.refreshEvent(currentEvent) } }
    }

    private val _goals = MutableLiveData<List<EventGoal>>()
    val goals: LiveData<List<EventGoal>> get() = _goals
    fun getGoals() {
        viewModelScope.launch { _goals.postValue(runCatching { repository.goals(currentEvent) }.getOrDefault(emptyList())) }
    }

    private val _schedule = MutableLiveData<List<EventScheduleItem>>()
    val schedule: LiveData<List<EventScheduleItem>> get() = _schedule
    fun getSchedule() {
        viewModelScope.launch { _schedule.postValue(runCatching { repository.schedule(currentEvent) }.getOrDefault(emptyList())) }
    }

    private val _zones = MutableLiveData<List<EventZone>>()
    val zones: LiveData<List<EventZone>> get() = _zones
    fun getZones() {
        viewModelScope.launch { _zones.postValue(runCatching { repository.zones(currentEvent) }.getOrDefault(emptyList())) }
    }

    private val _members = MutableLiveData<List<EventMember>>()
    val members: LiveData<List<EventMember>> get() = _members
    private var after: Long? = null
    var foundAll = false
    var new = true
    var searching = false

    fun getEventMembers(username: String? = null, new: Boolean = false) {
        viewModelScope.launch {
            if (new) { after = null; foundAll = false; this@ShowEventViewModel.new = true }
            val page = runCatching { repository.members(currentEvent, username?.takeIf { it.isNotBlank() }, after) }
                .getOrNull() ?: run { searching = false; return@launch }
            foundAll = !page.hasMore
            page.items.lastOrNull()?.let { after = it.userId }
            _members.postValue(page.items)
            searching = false
        }
    }

    private val _membersPreview = MutableLiveData<List<EventMember>>()
    val membersPreview: LiveData<List<EventMember>> get() = _membersPreview
    fun getMembersPreview() {
        viewModelScope.launch {
            _membersPreview.postValue(runCatching { repository.membersPreview(currentEvent) }.getOrDefault(emptyList()))
        }
    }

    fun joinEvent(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { repository.join(currentEvent) }.getOrDefault(false)
            if (ok) runCatching { repository.refreshEvent(currentEvent) }
            callback(ok)
        }
    }

    fun leaveEvent(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { repository.leave(currentEvent) }.getOrDefault(false)
            if (ok) runCatching { repository.refreshEvent(currentEvent) }
            callback(ok)
        }
    }

    fun setUserRole(userId: Long, role: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch { callback(runCatching { repository.setRole(currentEvent, userId, role) }.getOrDefault(false)) }
    }

    fun validateUser(userId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(runCatching { repository.validate(currentEvent, userId).validated }.getOrDefault(false))
        }
    }

    fun deleteEvent(callback: (Boolean) -> Unit) {
        viewModelScope.launch { callback(runCatching { repository.delete(currentEvent) }.getOrDefault(false)) }
    }
}
