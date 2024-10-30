package com.gy.ecotrace.ui.more.profile

import android.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.gy.ecotrace.Globals
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch


class ProfileViewModel(private val repository: Repository) : ViewModel() {
    private val _user = MutableLiveData<DatabaseMethods.UserDatabaseMethods.User>()
    val user: LiveData<DatabaseMethods.UserDatabaseMethods.User> get() = _user

    private val _graph = MutableLiveData<Bitmap?>()
    val graph: LiveData<Bitmap?> get() = _graph

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

    fun getUserGraphs(userId: String, time: Int = 0, hideFilters: MutableList<Int>? = null) {
        viewModelScope.launch {
            val graph = repository.getGraph(userId, time, hideFilters)
            if (graph != null) {
                _graph.postValue(graph)
            }
        }

    }

    private var fGot: String? = null
    var newFriends = true
    var updFriends = false
    fun getFriends(userId: String, new: Boolean = false) {
        viewModelScope.launch {
            if (new) {
                fGot = null
                _friends.value?.clear()
                newFriends = true
            }
            val friends = repository.getUserFriends(userId, fGot)
            fGot = try {friends.last().userId} catch (e: Exception) { null }

            val prevFriends = _friends.value ?: mutableListOf()
            _friends.postValue(prevFriends.plus(friends).toMutableList())
        }
    }

    private val _friend = MutableLiveData<Int>()
    val friend: LiveData<Int> get() = _friend
    fun areFriends(userId: String) {
        viewModelScope.launch {
            _friend.postValue(repository.areUsersFriends(userId))
        }
    }

    private var eGot: String? = null
    var newEvents = true
    var updEvents = false
    var foundAllEvents = false
    fun getEvents(userId: String, sortType: Int = 0, new: Boolean = false) {
        viewModelScope.launch {
            if (new) {
                eGot = null
                _events.value?.clear()
                newEvents = true
            }
            val events = repository.getUserEvents(userId, eGot, sortType)

            eGot = try {events?.last()?.eventInfo?.eventId} catch (e: Exception) { null }

            Log.d("events", "$eGot   $events")

            if (eGot == null) foundAllEvents = true

            _events.postValue(events ?: mutableListOf())
        }
    }


    private var gGot: String? = null
    fun getGroups(userId: String, new: Boolean = false) {
        viewModelScope.launch {
            if (new) {
                gGot = null
                _groups.value?.clear()
            }
            val groups = repository.getUserGroups(userId, gGot)
            gGot = try {groups.last().groupInfo.groupId} catch (e: Exception) { null }

            val prevGroups = _groups.value ?: mutableListOf()
            _groups.postValue(prevGroups.plus(groups).toMutableList())
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