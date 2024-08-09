package com.gy.ecotrace.ui.more.profile

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: Repository) : ViewModel() {
    private val _user = MutableLiveData<DatabaseMethods.UserDatabaseMethods.User>()
    val user: LiveData<DatabaseMethods.UserDatabaseMethods.User> get() = _user

    private val _friends = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Friendship>>()
    val friends: LiveData<MutableList<DatabaseMethods.DataClasses.Friendship>> get() = _friends

    private val _events = MutableLiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserEvent>>()
    val events: LiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserEvent>> get() = _events

    private val _groups = MutableLiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserGroup>>()
    val groups: LiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserGroup>> get() = _groups

    fun getUser(userId: String) {
        viewModelScope.launch {
            val user = repository.getUser(userId)
            if (user != null) {
                _user.postValue(user!!)
            }
        }
    }

    private var fGot: String? = null
    fun getFriends(userId: String) {
        viewModelScope.launch {
            val friends = repository.getUserFriends(userId, fGot)
            fGot = friends?.last()?.userId
            if (fGot != null) {
                val prevFriends = _friends.value ?: mutableListOf()
                _friends.postValue(prevFriends.plus(friends!!).toMutableList())
            }
        }
    }

    private var eGot: String? = null
    fun getEvents(userId: String) {
        viewModelScope.launch {
            val events = repository.getUserEvents(userId, eGot)
            eGot = events?.last()?.eventInfo?.eventId
            if (eGot != null) {
                val prevEvents = _events.value ?: mutableListOf()
                _events.postValue(prevEvents.plus(events!!).toMutableList())
            }
        }
    }

    private var gGot: String? = null
    fun getGroups(userId: String) {
        viewModelScope.launch {
            val groups = repository.getUserGroups(userId, gGot)
            gGot = groups?.last()?.groupInfo?.groupId
            if (eGot != null) {
                val prevGroups = _groups.value ?: mutableListOf()
                _groups.postValue(prevGroups.plus(groups!!).toMutableList())
            }
        }
    }

    fun removeFriend(userId: String) {
        viewModelScope.launch {
            repository.removeFriend(userId)
        }
    }
    fun addFriend(userId: String) {
        viewModelScope.launch {
            repository.addFriend(userId)
        }
    }
}