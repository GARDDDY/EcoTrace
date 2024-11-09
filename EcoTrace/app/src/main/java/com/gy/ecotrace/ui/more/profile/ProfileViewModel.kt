package com.gy.ecotrace.ui.more.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.dto.FriendItem
import com.gy.ecotrace.network.dto.GraphData
import com.gy.ecotrace.network.dto.RemoteUser
import com.gy.ecotrace.network.dto.UserEventItem
import com.gy.ecotrace.network.dto.UserGroupItem
import com.gy.ecotrace.repos.EventRepository
import com.gy.ecotrace.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    var currentUserId: Long = -1L
    var isSelf: Boolean = true

    var lastGraphTime = 0
    var lastGraphTypes: MutableList<Int>? = null
    var lastEventSort = 0
    var lastGroupSort = 0

    private val _profile = MutableLiveData<RemoteUser?>()
    val profile: LiveData<RemoteUser?> get() = _profile

    private val _graph = MutableLiveData<GraphData?>()
    val graph: LiveData<GraphData?> get() = _graph

    private val _friends = MutableLiveData<MutableList<FriendItem>>()
    val friends: LiveData<MutableList<FriendItem>> get() = _friends

    private val _events = MutableLiveData<MutableList<UserEventItem>>()
    val events: LiveData<MutableList<UserEventItem>> get() = _events

    private val _groups = MutableLiveData<MutableList<UserGroupItem>>()
    val groups: LiveData<MutableList<UserGroupItem>> get() = _groups

    private val _friendship = MutableLiveData<Int>()
    val friendship: LiveData<Int> get() = _friendship


    // 0 -- Profile, 1 -- Events, 2 -- Friends, 3 -- Groups
    private val _contentId = MutableLiveData(0)
    val contentId: LiveData<Int> get() = _contentId
    fun setContentId(id: Int) {
        if (_contentId.value != id) _contentId.value = id
    }

    private var collectingSelf = false

    fun loadProfile(userId: Long, isSelf: Boolean) {
        if (isSelf && !collectingSelf) {
            collectingSelf = true
            viewModelScope.launch {
                repository.observeCurrentUser().collect { if (it != null) _profile.postValue(it) }
            }
        }
        refreshProfile(userId, isSelf)
    }

    fun refreshProfile(userId: Long, isSelf: Boolean) {
        viewModelScope.launch {
            if (isSelf) {
                if (runCatching { repository.refreshCurrentUser() }.isFailure && _profile.value == null) {
                    _profile.postValue(null)
                }
            } else {
                _profile.postValue(runCatching { repository.getUser(userId) }.getOrNull())
            }
        }
    }

    var graphReq = true
    fun loadGraph(userId: Long, time: Int = 0, excludeTypes: List<Int>? = null) {
        viewModelScope.launch {
            graphReq = false
            _graph.postValue(runCatching { repository.getGraph(userId, time, excludeTypes) }.getOrNull())
        }
    }

    private var friendsAfter: Long? = null
    var newFriends = true
    var updFriends = false
    var foundAllFriends = false
    fun loadFriends(userId: Long, reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) { friendsAfter = null; foundAllFriends = false; newFriends = true }
            val page = runCatching { repository.getFriends(userId, after = friendsAfter) }.getOrNull()
                ?: run { updFriends = false; return@launch }
            friendsAfter = page.items.lastOrNull()?.id
            if (!page.hasMore) foundAllFriends = true
            val prev = if (reset) emptyList() else (_friends.value ?: emptyList())
            _friends.postValue((prev + page.items).toMutableList())
            updFriends = false
        }
    }

    fun loadFriendship(userId: Long) {
        viewModelScope.launch {
            _friendship.postValue(runCatching { repository.getFriendship(userId).code }.getOrDefault(0))
        }
    }

    fun addFriend(userId: Long) { viewModelScope.launch { runCatching { repository.addFriend(userId) } } }
    fun removeFriend(userId: Long) { viewModelScope.launch { runCatching { repository.removeFriend(userId) } } }

    private var eventsAfter: Long? = null
    var newEvents = true
    var updEvents = false
    var foundAllEvents = false
    fun loadEvents(userId: Long, sort: String? = null, reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) { eventsAfter = null; foundAllEvents = false; newEvents = true }
            val page = runCatching { repository.getEvents(userId, sort, eventsAfter) }.getOrNull()
                ?: run { updEvents = false; return@launch }
            eventsAfter = page.items.lastOrNull()?.id
            if (!page.hasMore) foundAllEvents = true
            val prev = if (reset) emptyList() else (_events.value ?: emptyList())
            _events.postValue((prev + page.items).toMutableList())
            updEvents = false
        }
    }

    private var groupsAfter: Long? = null
    var newGroups = true
    var updGroups = false
    var foundAllGroups = false
    fun loadGroups(userId: Long, sort: String? = null, reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) { groupsAfter = null; foundAllGroups = false; newGroups = true }
            val page = runCatching { repository.getGroups(userId, sort, groupsAfter) }.getOrNull()
                ?: run { updGroups = false; return@launch }
            groupsAfter = page.items.lastOrNull()?.id
            if (!page.hasMore) foundAllGroups = true
            val prev = if (reset) emptyList() else (_groups.value ?: emptyList())
            _groups.postValue((prev + page.items).toMutableList())
            updGroups = false
        }
    }

    fun leaveEvent(eventId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { eventRepository.leave(eventId) }.getOrDefault(false)
            callback(ok)
        }
    }
}
