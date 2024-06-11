package com.gy.ecotrace.ui.more.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: Repository) : ViewModel() {
    private val _user = MutableLiveData<DatabaseMethods.UserDatabaseMethods.UserInfo>()
    val user: LiveData<DatabaseMethods.UserDatabaseMethods.UserInfo> get() = _user

    private val _events = MutableLiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserActivity>>()
    val events: LiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserActivity>> get() = _events

    private val _friends = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Friendship>>()
    val friends: LiveData<MutableList<DatabaseMethods.DataClasses.Friendship>> get() = _friends

    private val _groups = MutableLiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserGroups>>()
    val groups: LiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserGroups>> get() = _groups

    private var maxEvents = 0
    private var maxFriends = 0
    private var maxGroups = 0
    private val _additionalData = MediatorLiveData<Boolean>().apply {
        var userLoaded = false
        var eventsLoaded = false // 3
        var friendsLoaded = false // 6
        var groupsLoaded = false // 2

        fun updateValue() {
            value = userLoaded && eventsLoaded && friendsLoaded && groupsLoaded
        }

        addSource(_user) {
            userLoaded = true
            updateValue()
        }
        addSource(_events) {
            eventsLoaded = maxEvents + it.size >= 6 || maxEvents < 3
            updateValue()
        }
        addSource(_friends) {
            friendsLoaded = maxFriends + it.size >= 12 || maxFriends < 6
            updateValue()
        }
        addSource(_groups) {
            groupsLoaded = maxGroups + it.size >= 4 || maxGroups < 2
            updateValue()
        }
    }
    val additionalData: LiveData<Boolean> get() = _additionalData

    private fun getMaximums(userId: String) {
        viewModelScope.launch {
            val maximums = repository.getMaximums(userId)
            maxEvents = maximums[0]
            maxFriends = maximums[1]
            maxGroups = maximums[2]
        }
    }

    fun getUser(userId: String) {
        viewModelScope.launch {
            getMaximums(userId)
            val user = repository.getUser(userId)
            _user.postValue(user)
        }
    }

    fun getEvents(userId: String) {
        viewModelScope.launch {
            val events = repository.getEvents(userId)
            _events.postValue(events)
        }
    }

    fun getFriends(userId: String) {
        viewModelScope.launch {
            val friends = repository.getFriends(userId)
            _friends.postValue(friends)
        }
    }

    fun observeGroups(userId: String) {
        val groupsMap = mutableMapOf<String, DatabaseMethods.UserDatabaseMethods.UserGroups>()
        viewModelScope.launch {
            val data = repository.getGroups(userId)
            for (group in data) {
                groupsMap[group.groupInfo.groupId] = group
            }
            _groups.postValue(groupsMap.values.toMutableList().sortedBy { it.groupInfo.groupCountMembers }.reversed().toMutableList())
            for (group in data) {
                repository.observeGroupMembers(group.groupInfo.groupId) { updatedGroupInfo ->
                    val userGroupClass = repository._group()
                    userGroupClass.isUserInGroup = group.isUserInGroup
                    userGroupClass.groupInfo = updatedGroupInfo!!
                    groupsMap[group.groupInfo.groupId] = userGroupClass
                    _groups.postValue(groupsMap.values.toMutableList().sortedBy { it.groupInfo.groupCountMembers }.reversed().toMutableList())
                }
            }
        }
    }

    fun removeFriend(userId1: String, userId2: String) {
        viewModelScope.launch {
            repository.removeFriend(userId1, userId2)
        }
    }
    fun addFriend(userId1: String, userId2: String) {
        viewModelScope.launch {
            repository.addFriend(userId1, userId2)
        }
    }
}