package com.gy.ecotrace.ui.more.groups.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.ui.more.groups.additional.GroupRepository
import kotlinx.coroutines.launch

class ShowGroupMembersViewModel(private val repository: Repository): ViewModel() {
    private val _users = MutableLiveData<HashMap<String, HashMap<String, Boolean>>>()
    val users: LiveData<HashMap<String, HashMap<String, Boolean>>> get() = _users
    var lastMemberId: String? = null
    fun getAndObserveMembers(groupId: String) {
        viewModelScope.launch {
            val data = repository.getGroupUsers(groupId, lastMemberId)
            lastMemberId = data.first
            _users.postValue(data.second)
        }
    }

    fun setUserRole(groupId: String, userId: String, roleFrom: Int, roleTo: Int) {
        repository.setUserRoleInGroup(groupId, userId, roleFrom, roleTo)
    }
    fun kickUser(groupId: String, userId: String, role: Int) {
        repository.kickUserFromGroup(groupId, userId, role)
    }

    fun roleToAbilities(role: Int): DatabaseMethods.DataClasses.UserGroupAbilities {
        return GroupRepository.Functions().userAbilitiesInGroup(role)
    }

    fun getUsername(userId: String, callback: (String) -> Unit) {
        viewModelScope.launch {
            callback(GroupRepository.Functions().getUsernameOnly(userId))
        }
    }
}