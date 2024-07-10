package com.gy.ecotrace.ui.more.events

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.db.DatabaseMethods
import kotlinx.coroutines.launch

class ShowEventViewModel(private val repository: Repository) : ViewModel() {
//    private val _members = MutableLiveData<MutableList<AppRepository.UserEventInfoShort>>()
//    val members: LiveData<MutableList<AppRepository.UserEventInfoShort>> get() = _members
    private val _members = MutableLiveData<Repository.UserEventInfoShort>()
    val members: LiveData<Repository.UserEventInfoShort> get() = _members

    private val _event = MutableLiveData<DatabaseMethods.DataClasses.Event>()
    val event: LiveData<DatabaseMethods.DataClasses.Event> get() = _event

    fun getEventMembers(event: DatabaseMethods.DataClasses.Event) {
        viewModelScope.launch {
            event.eventUsersToTheirRoles!!.forEach { (memberId, values) ->
                val userClass = repository.getUserShortInfoForEventMembersLayout(memberId)
                userClass.userRole = values
                _members.postValue(userClass)
            }
        }
    }

    fun getEvent(eventId: String) {
        viewModelScope.launch {
            val event = repository.getEvent(eventId)
            _event.postValue(event)
        }
    }

    fun joinEvent(eventId: String, userId: String) {
        viewModelScope.launch {
            repository.joinEvent(eventId, userId)
            val newEvent = event.value!!
            newEvent.eventCountMembers += 1
            newEvent.eventUsersToTheirRoles!![userId] = 2
            _event.postValue(newEvent)
        }
    }
    fun leaveEvent(eventId: String, userId: String) {
        viewModelScope.launch {
            repository.leaveEvent(eventId, userId)
            val newEvent = event.value!!
            newEvent.eventCountMembers -= 1
            newEvent.eventUsersToTheirRoles!!.remove(userId)
            _event.postValue(newEvent)
        }
    }

    private val _eventmore = MutableLiveData<DatabaseMethods.ApplicationDatabaseMethods.EventMore>()
    val eventmore: LiveData<DatabaseMethods.ApplicationDatabaseMethods.EventMore> get() = _eventmore

    fun getEventMore(eventId: String) {
        viewModelScope.launch {
            Log.d("hjhjk", "here")
            val eventmore = repository.getEventMore(eventId)
            Log.d("hjhjk", eventmore.toString())
            _eventmore.postValue(eventmore)
        }
    }

}