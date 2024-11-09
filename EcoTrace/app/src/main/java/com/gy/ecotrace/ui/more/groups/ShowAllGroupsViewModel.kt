package com.gy.ecotrace.ui.more.groups

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.network.dto.RemoteGroup
import com.gy.ecotrace.network.dto.UserGroupItem
import com.gy.ecotrace.repos.GroupRepository
import com.gy.ecotrace.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShowAllGroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _groups = MutableLiveData<List<RemoteGroup>>()
    val groups: LiveData<List<RemoteGroup>> get() = _groups

    var filter: String? = null
    var name: String? = null
    var sortBy: String? = null
    private var after: Long? = null
    var foundAll = false
    var isUpdating = false

    fun search(reset: Boolean = true) {
        viewModelScope.launch {
            if (reset) { after = null; foundAll = false }
            val page = runCatching { groupRepository.list(filter, after, name, sortBy) }.getOrNull()
            isUpdating = false
            if (page == null) { _groups.postValue(emptyList()); return@launch }
            foundAll = !page.hasMore
            page.items.lastOrNull()?.let { after = it.id }
            _groups.postValue(page.items)
        }
    }

    private val _userGroups = MutableLiveData<List<UserGroupItem>>()
    val userGroups: LiveData<List<UserGroupItem>> get() = _userGroups

    fun loadUserGroups(userId: Long) {
        viewModelScope.launch {
            val page = runCatching { userRepository.getGroups(userId) }.getOrNull() ?: return@launch
            _userGroups.postValue(page.items)
        }
    }

    fun leaveGroup(groupId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { groupRepository.leave(groupId) }.getOrDefault(false)
            callback(ok)
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
}
