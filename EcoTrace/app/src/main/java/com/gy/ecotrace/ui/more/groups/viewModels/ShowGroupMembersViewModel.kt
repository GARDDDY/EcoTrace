package com.gy.ecotrace.ui.more.groups.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.dto.GroupMember
import com.gy.ecotrace.repos.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowGroupMembersViewModel @Inject constructor(
    private val repository: GroupRepository
) : ViewModel() {

    var groupId: Long = 0
    var myRole: String? = null

    private val _creator = MutableLiveData<GroupMember?>()
    val creator: LiveData<GroupMember?> get() = _creator
    private val _managers = MutableLiveData<List<GroupMember>>()
    val managers: LiveData<List<GroupMember>> get() = _managers
    private val _assistants = MutableLiveData<List<GroupMember>>()
    val assistants: LiveData<List<GroupMember>> get() = _assistants
    private val _participants = MutableLiveData<List<GroupMember>>()
    val participants: LiveData<List<GroupMember>> get() = _participants

    private val _roleReady = MutableLiveData<String?>()
    val roleReady: LiveData<String?> get() = _roleReady

    data class TopActiveParticipants(val items: List<GroupMember> = emptyList(), val total: Int = 0)
    private val _topActiveParticipants = MutableLiveData<TopActiveParticipants>()
    val topActiveParticipants: LiveData<TopActiveParticipants> get() = _topActiveParticipants

    private val _searchResults = MutableLiveData<List<GroupMember>>()
    val searchResults: LiveData<List<GroupMember>> get() = _searchResults

    var foundAll = false
    private var after: Long? = null

    fun loadRole() {
        viewModelScope.launch {
            myRole = runCatching {
                repository.cachedGroup(groupId)?.myRole ?: repository.refreshGroup(groupId).myRole
            }.getOrNull()
            _roleReady.postValue(myRole)
        }
    }

    fun loadTopActiveParticipants() {
        viewModelScope.launch {
            val page = runCatching {
                repository.members(groupId, role = "participant", sort = "posts_desc", limit = 7)
            }.getOrNull()
            _topActiveParticipants.postValue(TopActiveParticipants(page?.items ?: emptyList(), page?.total ?: 0))
        }
    }

    fun searchParticipants(name: String?) {
        viewModelScope.launch {
            val page = runCatching {
                repository.members(groupId, role = "participant", name = name?.takeIf { it.isNotBlank() })
            }.getOrNull()
            _searchResults.postValue(page?.items ?: emptyList())
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _creator.postValue(runCatching { repository.members(groupId, "creator").items.firstOrNull() }.getOrNull())
        }
        viewModelScope.launch {
            _managers.postValue(runCatching { repository.members(groupId, "manager").items }.getOrDefault(emptyList()))
        }
        viewModelScope.launch {
            _assistants.postValue(runCatching { repository.members(groupId, "assistant").items }.getOrDefault(emptyList()))
        }
        loadParticipants(reset = true)
    }

    fun loadParticipants(reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) { after = null; foundAll = false }
            val page = runCatching { repository.members(groupId, "participant", after) }.getOrNull() ?: return@launch
            foundAll = !page.hasMore
            page.items.lastOrNull()?.let { after = it.userId }
            _participants.postValue(page.items)
        }
    }

    fun setRole(userId: Long, role: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch { callback(runCatching { repository.setRole(groupId, userId, role) }.getOrDefault(false)) }
    }

    fun kick(userId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(runCatching { repository.kick(groupId, userId); true }.getOrDefault(false))
        }
    }
}
