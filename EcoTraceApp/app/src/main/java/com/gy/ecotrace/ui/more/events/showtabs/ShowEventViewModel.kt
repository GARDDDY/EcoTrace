package com.gy.ecotrace.ui.more.events.showtabs

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.auth.User
import com.gy.ecotrace.db.Repository
import com.gy.ecotrace.db.DatabaseMethods
import kotlinx.coroutines.launch

class ShowEventViewModel(private val repository: Repository) : ViewModel() {

    var currentEvent = "0"

//    private val _members = MutableLiveData<MutableList<AppRepository.UserEventInfoShort>>()
//    val members: LiveData<MutableList<AppRepository.UserEventInfoShort>> get() = _members
    private val _members = MutableLiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserActivity>>()
    val members: LiveData<MutableList<DatabaseMethods.UserDatabaseMethods.UserActivity>> get() = _members

    private val _event = MutableLiveData<DatabaseMethods.UserDatabaseMethods.UserEvent>()
    val event: LiveData<DatabaseMethods.UserDatabaseMethods.UserEvent> get() = _event

    private var startAfter: String? = null
    var foundAll = false
    fun getEventMembers(username: String? = null) {
        viewModelScope.launch {
            val members = repository.getEventMembers(currentEvent, startAfter, username)

            _members.postValue(members)

        }
    }

    fun isUserModer(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            callback(repository.isUserModerInEvent(currentEvent))
        }
    }

    fun getEvent() {
        viewModelScope.launch {
            val event = repository.getUserEvent(currentEvent)
            _event.postValue(event)
        }
    }

    fun joinEvent() {
        viewModelScope.launch {
            repository.joinEvent(currentEvent)

        }
    }
    fun leaveEvent() {
        viewModelScope.launch {
            repository.leaveEvent(currentEvent)

        }
    }

    private val _eventGoals = MutableLiveData<MutableList<String>>()
    val eventGoals: LiveData<MutableList<String>> get() = _eventGoals

    fun getGoals() {
        viewModelScope.launch {
            _eventGoals.postValue(repository.getEventGoals(currentEvent))
        }
    }

    private val _eventTimes = MutableLiveData<HashMap<String, String>>()
    val eventTimes: LiveData<HashMap<String, String>> get() = _eventTimes

    fun getTimes() {
        viewModelScope.launch {
            _eventTimes.postValue(repository.getEventTimes(currentEvent))
        }
    }

    private val _eventCoords = MutableLiveData<MutableList<DatabaseMethods.DataClasses.MapObject>>()
    val eventCoords: LiveData<MutableList<DatabaseMethods.DataClasses.MapObject>> get() = _eventCoords

    fun getCoords() {
        viewModelScope.launch {
            _eventCoords.postValue(repository.getEventCoords(currentEvent))
        }
    }

}