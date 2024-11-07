package com.gy.ecotrace.ui.more.groups.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class ShowGroupMembersViewModel(private val repository: Repository): ViewModel() {

    lateinit var groupId: String

    private val _creator = MutableLiveData<DatabaseMethods.DataClasses.UserInGroup>()
    val creator: LiveData<DatabaseMethods.DataClasses.UserInGroup> get() = _creator

    fun getCreator() {
        viewModelScope.launch {
            val data = repository.getGroupUsers(groupId, lastFound, 0)

            val creator = data.second[0]
            _creator.postValue(creator)
        }
    }

    private val _ceo = MutableLiveData<MutableList<DatabaseMethods.DataClasses.UserInGroup>>()
    val ceo: LiveData<MutableList<DatabaseMethods.DataClasses.UserInGroup>> get() = _ceo

    fun getCEOs() {
        viewModelScope.launch {
            val data = repository.getGroupUsers(groupId, lastFound, 1)

            val members = data.second
            _ceo.postValue(members)
        }
    }

    private val _helpers = MutableLiveData<MutableList<DatabaseMethods.DataClasses.UserInGroup>>()
    val helpers: LiveData<MutableList<DatabaseMethods.DataClasses.UserInGroup>> get() = _helpers

    fun getHelpers() {
        viewModelScope.launch {
            val data = repository.getGroupUsers(groupId, lastFound, 2)

            val members = data.second
            _helpers.postValue(members)
        }
    }

    private val _users = MutableLiveData<MutableList<DatabaseMethods.DataClasses.UserInGroup>>()
    val users: LiveData<MutableList<DatabaseMethods.DataClasses.UserInGroup>> get() = _users

    var foundAll = false
    private var lastFound: String? = null

    fun getMembers() {
        viewModelScope.launch {
            val data = repository.getGroupUsers(groupId, lastFound, 3)

            foundAll = data.first

            val members = data.second
            lastFound = if (members.isNotEmpty()) members.last().userId else null
            _users.postValue(members)
        }
    }

    fun setUserRole(userId: String, role: Int, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.setUserRoleInGroup(groupId, userId, role))
        }
    }

    fun kickUser(userId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.kickUserFromGroup(groupId, userId))
        }
    }

    fun getRole(callback: (Int) -> Unit) {
        viewModelScope.launch {
            callback(repository.getUserRoleInGroup(groupId))
        }
    }
}