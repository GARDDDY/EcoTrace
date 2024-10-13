package com.gy.ecotrace.ui.more.events

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gy.ecotrace.Globals
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository
import kotlinx.coroutines.launch

class ShowAllEventsViewModel(private val repository: Repository) : ViewModel() {

    var isUpdating = false

    private val _events = MutableLiveData<HashMap<String, String>?>()
    private var lastId: String? = null
    val events: LiveData<HashMap<String, String>?> get() = _events

    private val filtersSearchNew = MutableList(Globals.getInstance().getEventsFilters().size) { false }
    fun reapplyFilter(filterIndex: Int) {
        filtersSearchNew[filterIndex] = !filtersSearchNew[filterIndex]
    }

    private val _eventFoundFilter = MutableLiveData<MutableList<DatabaseMethods.DataClasses.Event>>()
    val eventsFound: LiveData<MutableList<DatabaseMethods.DataClasses.Event>> get() = _eventFoundFilter
    private var startAtId: String? = null
    var foundAll = false


    fun getEvents(updateAllGot: Boolean = true) {
        viewModelScope.launch {
            if (updateAllGot) {
                startAtId = null
                foundAll = false
            }

            val filters = filtersSearchNew.mapIndexedNotNull {
                index, value -> if (value) (index + 1).toString() else null
            }.joinToString(",")


            val data = repository.findEventsWithFilters(filters, startAtId)

            Log.d("data", data.toString())

            startAtId = data.first.first
            foundAll = data.first.second
            val events = data.second

            _eventFoundFilter.postValue(events)
        }
    }

    fun getUserEvents() {
        viewModelScope.launch {
            Log.d("test", "1")
            val events = repository.getUserEventsShort(lastId)
            Log.d("test", "2")
            lastId = events?.keys?.last()
            Log.d("test", "3")
            _events.postValue(events)
        }
    }

}