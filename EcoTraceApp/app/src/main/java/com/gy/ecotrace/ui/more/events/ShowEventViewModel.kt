package com.gy.ecotrace.ui.more.events

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

    fun getEventMembers(eventId: String, event: DatabaseMethods.DataClasses.Event) {
        viewModelScope.launch {
            event.eventUsersToTheirNames!!.forEach { (memberId, values) ->
                val userClass = repository.getUserShortInfoForEventMembersLayout(memberId)
                userClass.userRole = values.role
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

}