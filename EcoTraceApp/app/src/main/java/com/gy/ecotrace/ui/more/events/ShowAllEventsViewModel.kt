package com.gy.ecotrace.ui.more.events

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class ShowAllEventsViewModel(private val repository: Repository) : ViewModel() {
    private val filtersSearchNew = MutableList(DatabaseMethods.DataClasses.UserFiltersSearchBy.size) { false }
    fun reapplyFilter(filterIndex: Int) {
        filtersSearchNew[filterIndex] = !filtersSearchNew[filterIndex]
    }

    private val _eventFoundFilter = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Event>>()
    val eventsFound: LiveData<MutableList<DatabaseMethods.DataClasses.Event>> get() = _eventFoundFilter
    var lastId: Pair<Boolean, String?> = Pair(true, null)
    private var showedEvents: MutableList<String> = mutableListOf()
    fun getEvents(needNew: Boolean = false, searchWithString: String? = null) {
        viewModelScope.launch {
            if (needNew) {
                _eventFoundFilter.value = mutableListOf()
                lastId = Pair(true, null)
                showedEvents = mutableListOf()
            }

            val filters = mutableListOf<Int>()
            filtersSearchNew.forEachIndexed { index, it ->
                if (it) {
                    filters.add(index)
                }
            }

            val events = repository.findEventsWithFilters(filters, lastId, searchWithString)
            lastId = events.second
            val currents = _eventFoundFilter.value ?: mutableListOf()
            val newEvents = events.first.filter { event ->
                !showedEvents.contains(event.eventId) && !currents.any { it.eventId == event.eventId }
            }
            newEvents.forEach {
                it.eventCreatorName = getUsernameOnly(it.eventCreatorId)
            }
            currents.addAll(newEvents)
            val distinctList = currents.distinct()
            currents.clear()
            currents.addAll(distinctList)
            _eventFoundFilter.postValue(currents)
            showedEvents.addAll(newEvents.map { it.eventId })
        }
    }

    private suspend fun getUsernameOnly(userId: String): String {
        return repository.getUsernameOnly(userId)
    }


}